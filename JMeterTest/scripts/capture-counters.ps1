# 抓取 Redis/MySQL 计数器终态并追加到实验 counters.csv
# 用法: .\capture-counters.ps1 -Exp <arm目录> -Scenario <场景名> -Tag <轮次标签>
param(
    [Parameter(Mandatory = $true)][string]$Exp,
    [Parameter(Mandatory = $true)][string]$Scenario,
    [Parameter(Mandatory = $true)][string]$Tag
)
$ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$stats = (docker exec campushub-redis redis-cli --user root -a 123456 --no-auth-warning INFO stats) -join "`n"
$clients = (docker exec campushub-redis redis-cli --user root -a 123456 --no-auth-warning INFO clients) -join "`n"
$mysql = (docker exec campushub-mysql mysql -uroot -p123456 -N -e "SHOW GLOBAL STATUS LIKE 'Com_select'" 2>$null) -join "`n"
$tcp = [regex]::Match($stats, 'total_commands_processed:(\d+)').Groups[1].Value
$hit = [regex]::Match($stats, 'keyspace_hits:(\d+)').Groups[1].Value
$mis = [regex]::Match($stats, 'keyspace_misses:(\d+)').Groups[1].Value
$cc = [regex]::Match($clients, 'connected_clients:(\d+)').Groups[1].Value
$cs = [regex]::Match($mysql, 'Com_select\s+(\d+)').Groups[1].Value
$dir = Join-Path $Exp "$Scenario\counters"
New-Item -ItemType Directory -Force -Path $dir | Out-Null
$file = Join-Path $dir "counters.csv"
if (-not (Test-Path $file)) {
    Add-Content -Path $file -Value "ts,scenario,tag,total_commands_processed,keyspace_hits,keyspace_misses,connected_clients,Com_select" -Encoding utf8
}
$line = "$ts,$Scenario,$Tag,$tcp,$hit,$mis,$cc,$cs"
Add-Content -Path $file -Value $line -Encoding utf8
$line
