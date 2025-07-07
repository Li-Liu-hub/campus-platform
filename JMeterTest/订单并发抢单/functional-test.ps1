# 订单模块功能测试脚本：CRUD + 幂等 + 分布式锁抢单
# 用法：powershell -File functional-test.ps1 [-BaseUrl http://localhost:8081]
param(
    [string]$BaseUrl = 'http://localhost:8080'
)

$ErrorActionPreference = 'Stop'
$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$userA = "ordera$stamp"
$userB = "orderb$stamp"
$script:pass = 0
$script:fail = 0

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

# 期望业务失败的调用：返回 @{ status = HTTP状态码; text = 响应体 }
function Invoke-JsonExpectError([string]$Method, [string]$Path, $Body, [string]$Token) {
    try {
        $resp = Invoke-Json $Method $Path $Body $Token $null
        return @{ status = 200; text = ($resp | ConvertTo-Json -Depth 5) }
    } catch {
        $response = $_.Exception.Response
        $status = [int]$response.StatusCode
        # pwsh7 抛出的是 HttpResponseMessage，响应体统一从 ErrorDetails 取，5.1 再回退流读取
        $text = if ($_.ErrorDetails.Message) { $_.ErrorDetails.Message } else {
            $stream = $response.GetResponseStream()
            $reader = New-Object IO.StreamReader($stream, [Text.Encoding]::UTF8)
            $reader.ReadToEnd()
        }
        return @{ status = $status; text = $text }
    }
}

function Assert([bool]$Condition, [string]$Name, [string]$Detail) {
    if ($Condition) {
        $script:pass++
        Write-Host ("[PASS] {0}" -f $Name)
    } else {
        $script:fail++
        Write-Host ("[FAIL] {0} => {1}" -f $Name, $Detail)
    }
}

# ---------- 准备：注册并登录两个用户 ----------
foreach ($name in @($userA, $userB)) {
    try {
        Invoke-Json 'Post' '/api/v1/auth/register' @{ userName = $name; userPassword = '123456' } $null $null | Out-Null
    } catch { }
}
$loginA = Invoke-Json 'Post' '/api/v1/auth/login' @{ userName = $userA; userPassword = '123456' } $null "dev-a-$stamp"
$loginB = Invoke-Json 'Post' '/api/v1/auth/login' @{ userName = $userB; userPassword = '123456' } $null "dev-b-$stamp"
$tokenA = $loginA.data.tokenValue
$tokenB = $loginB.data.tokenValue
$userIdA = $loginA.data.userId
$userIdB = $loginB.data.userId
Assert ($null -ne $tokenA -and $null -ne $tokenB) '两个用户登录成功' "tokenA=$tokenA, tokenB=$tokenB"

$timeoutText = (Get-Date).AddHours(2).ToString('yyyy-MM-ddTHH:mm:ss')

# ---------- 用例 1：创建订单 ----------
$idem1 = "idem-$stamp-1"
$created = Invoke-Json 'Post' '/api/v1/orders/create' @{
    orderType = '跑腿代取'; orderAmount = 9.9; orderTimeout = $timeoutText; orderIdempotencyKey = $idem1
} $tokenA $null
$orderId = $created.data.orderId
Assert ($created.code -eq 200 -and $null -ne $orderId) '创建订单成功' ($created | ConvertTo-Json -Depth 5)
Assert ($created.data.orderStatus -eq 0 -and $created.data.orderSentUserId -eq $userIdA) '新订单为待接单且发布人是 A' ($created | ConvertTo-Json -Depth 5)

# ---------- 用例 2：幂等键重复创建返回原订单 ----------
$again = Invoke-Json 'Post' '/api/v1/orders/create' @{
    orderType = '跑腿代取'; orderAmount = 9.9; orderTimeout = $timeoutText; orderIdempotencyKey = $idem1
} $tokenA $null
Assert ($again.data.orderId -eq $orderId) '相同幂等键重复创建返回原订单' "expect=$orderId actual=$($again.data.orderId)"

# ---------- 用例 3：A 抢自己的订单被拒绝 ----------
$selfGrab = Invoke-JsonExpectError 'Post' "/api/v1/orders/grab/$orderId" $null $tokenA
Assert ($selfGrab.status -eq 400) 'A 抢自己的订单返回 400' "status=$($selfGrab.status) body=$($selfGrab.text)"

# ---------- 用例 4：B 浏览订单，浏览量加一 ----------
$viewed = Invoke-Json 'Get' "/api/v1/orders/get/$orderId" $null $tokenB $null
Assert ($viewed.code -eq 200 -and $viewed.data.orderViewNumber -eq 1) 'B 浏览订单浏览量为 1' "viewNumber=$($viewed.data.orderViewNumber)"

# ---------- 用例 5：B 抢单成功 ----------
$grabbed = Invoke-Json 'Post' "/api/v1/orders/grab/$orderId" $null $tokenB $null
Assert ($grabbed.code -eq 200 -and $grabbed.data.orderStatus -eq 1 -and $grabbed.data.orderReceiveUserId -eq $userIdB) 'B 抢单成功且接单人回填' ($grabbed | ConvertTo-Json -Depth 5)

# ---------- 用例 6：订单已接单后再次抢返回 409（用接单人 B 再次抢，A 再抢会命中"抢自己订单"400） ----------
$reGrab = Invoke-JsonExpectError 'Post' "/api/v1/orders/grab/$orderId" $null $tokenB $null
Assert ($reGrab.status -eq 409) '已接单订单再次抢单返回 409' "status=$($reGrab.status) body=$($reGrab.text)"

# ---------- 用例 7：已接单订单不可修改/不可删除 ----------
$updateAccepted = Invoke-JsonExpectError 'Put' "/api/v1/orders/update/$orderId" @{ orderAmount = 19.9 } $tokenA $null
Assert ($updateAccepted.status -eq 409) '已接单订单修改返回 409' "status=$($updateAccepted.status)"
$deleteAccepted = Invoke-JsonExpectError 'Delete' "/api/v1/orders/delete/$orderId" $null $tokenA $null
Assert ($deleteAccepted.status -eq 409) '已接单订单删除返回 409' "status=$($deleteAccepted.status)"

# ---------- 用例 8：B 查询我接的订单能看到该单 ----------
$received = Invoke-Json 'Get' '/api/v1/orders/query?role=received' $null $tokenB $null
$hit = $received.data.orders | Where-Object { $_.orderId -eq $orderId }
Assert ($null -ne $hit) 'B 的 received 列表包含被抢订单' ($received | ConvertTo-Json -Depth 5)

# ---------- 用例 9：待接单订单可修改、可删除 ----------
$created2 = Invoke-Json 'Post' '/api/v1/orders/create' @{
    orderType = '代买代购'; orderAmount = 5.5; orderTimeout = $timeoutText; orderIdempotencyKey = "idem-$stamp-2"
} $tokenA $null
$orderId2 = $created2.data.orderId
$updated = Invoke-Json 'Put' "/api/v1/orders/update/$orderId2" @{ orderAmount = 6.6 } $tokenA $null
Assert ($updated.code -eq 200 -and [double]$updated.data.orderAmount -eq 6.6) '待接单订单修改金额成功' "amount=$($updated.data.orderAmount)"
Invoke-Json 'Delete' "/api/v1/orders/delete/$orderId2" $null $tokenA $null | Out-Null
$deleted = Invoke-JsonExpectError 'Get' "/api/v1/orders/get/$orderId2" $null $tokenA $null
Assert ($deleted.status -eq 404) '删除后查询返回 404' "status=$($deleted.status)"

# ---------- 用例 10：抢单后 Redis 锁键已释放 ----------
$cli = 'docker exec campushub-redis redis-cli --user root --pass 123456 --no-auth-warning'
$lockExists = Invoke-Expression "$cli EXISTS campushub:order:lock:$orderId"
Assert ("$lockExists" -eq '0') '抢单结束后锁键已从 Redis 释放' "EXISTS=$lockExists"

Write-Host ''
Write-Host ("结果：通过 {0} 项，失败 {1} 项" -f $script:pass, $script:fail)
if ($script:fail -gt 0) { exit 1 }
