# 订单并发抢单基准驱动脚本：3 轮，每轮新订单 + 100 线程抢购
# 用法：powershell -File run-grab-bench.ps1
$ErrorActionPreference = 'Stop'
$BaseUrl = 'http://localhost:8080'
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path

function Invoke-Json([string]$Method, [string]$Path, $Body, [string]$Token, [string]$DeviceId) {
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }
    if ($DeviceId) { $headers['X-Device-Id'] = $DeviceId }
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Depth 5
        return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -Headers $headers `
            -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($json))
    }
    return Invoke-RestMethod -Method $Method -Uri "$BaseUrl$Path" -Headers $headers
}

function Register-And-Login([string]$Name, [string]$DeviceId) {
    try {
        Invoke-Json 'Post' '/api/v1/auth/register' @{ userName = $Name; userPassword = '123456' } $null $null | Out-Null
    } catch { }
    $login = Invoke-Json 'Post' '/api/v1/auth/login' @{ userName = $Name; userPassword = '123456' } $null $DeviceId
    return @{ userId = $login.data.userId; token = $login.data.tokenValue }
}

$owner = Register-And-Login "owner$stamp" "dev-owner-$stamp"
$timeoutText = (Get-Date).AddHours(2).ToString('yyyy-MM-ddTHH:mm:ss')
$summary = @()

foreach ($round in 1..3) {
    # 准备本轮订单与 20 个抢购用户
    $created = Invoke-Json 'Post' '/api/v1/orders/create' @{
        orderType = '跑腿代取'; orderAmount = 9.9; orderTimeout = $timeoutText
        orderIdempotencyKey = "bench-$stamp-r$round"
    } $owner.token
    $orderId = $created.data.orderId

    $tokens = @()
    foreach ($i in 1..20) {
        $grabber = Register-And-Login "grab$("$stamp")r$round`_$i" "dev-g-$stamp-$round-$i"
        $tokens += $grabber.token
    }
    # token 文件写到 %TEMP%：-J 传中文路径给 JVM 会因编码问题打不开 CSV，导致线程 0 样本
    $tokenFile = Join-Path $env:TEMP "campushub-grab-tokens-r$round.txt"
    [IO.File]::WriteAllLines($tokenFile, $tokens)

    # 100 线程并发抢同一单
    # -J 参数必须用双引号字符串显式插值：PS5.1 脚本模式下裸词 -Jx=$var 不会展开，
    # 会把字面量 "$orderId" 传给 JVM，导致 CSV 路径非法、线程 0 样本
    $jtl = Join-Path $here "grab-r$round.jtl"
    $jlog = Join-Path $env:TEMP "campushub-jmeter-r$round.log"
    $jmx = Join-Path $here '订单并发抢单-100线程1轮.jmx'
    $jmeterArgs = @(
        '-n',
        '-t', $jmx,
        '-l', $jtl,
        '-j', $jlog,
        "-JorderId=$orderId",
        "-JtokenFile=$tokenFile",
        '-Jport=8080'
    )
    & jmeter @jmeterArgs 2>&1 | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "jmeter 第 $round 轮执行失败" }

    # 统计响应码分布与唯一成功
    $samples = Import-Csv $jtl
    $codes = $samples | Group-Object responseCode | ForEach-Object { "$($_.Name)=$($_.Count)" }
    $successCount = ($samples | Where-Object { $_.responseCode -eq '200' }).Count

    # 数据库校验：订单唯一且状态流转为已接单（容器内注入 MYSQL_PWD 规避命令行密码警告）
    $dbRow = docker exec -e MYSQL_PWD=123456 campushub-mysql mysql -uroot -N -e `
        "SELECT order_status, IFNULL(order_receive_user_id, -1) FROM campushub.ch_order WHERE order_id = $orderId"
    # Redis 校验：抢单结束后锁已释放
    $lockExists = docker exec campushub-redis redis-cli --user root --pass 123456 --no-auth-warning `
        EXISTS "campushub:order:lock:$orderId"

    $row = [pscustomobject]@{
        Round       = $round
        OrderId     = $orderId
        Samples     = $samples.Count
        Codes       = ($codes -join ', ')
        Success200  = $successCount
        DbStatus    = "$dbRow".Trim()
        LockExists  = "$lockExists".Trim()
    }
    $summary += $row
    $row | Format-List | Out-Host
}

Write-Host '===== 汇总 ====='
$summary | Format-Table -AutoSize | Out-Host
