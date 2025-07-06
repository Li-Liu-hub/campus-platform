#!/usr/bin/env gawk -f
# 用法: gawk -f summarize.awk result.jtl
# 统计 JMeter CSV 结果：样本数/错误率/平均/最小/最大/百分位/吞吐量
BEGIN { FS = "," }
NR == 1 { next }
{
    n++
    elapsed[n] = $2
    if ($8 == "false") err++
    sum += $2
    if (n == 1 || $2 < min) min = $2
    if ($2 > max) max = $2
    if (n == 1) firstTs = $1
    lastTs = $1
}
END {
    if (n == 0) { print "无样本"; exit }
    asort(elapsed)
    printf "样本数:      %d\n", n
    printf "错误数:      %d (%.2f%%)\n", err, err * 100 / n
    printf "平均值:      %.0f ms\n", sum / n
    printf "中位数:      %d ms\n", pct(50)
    printf "90%% 百分位:  %d ms\n", pct(90)
    printf "95%% 百分位:  %d ms\n", pct(95)
    printf "99%% 百分位:  %d ms\n", pct(99)
    printf "最小/最大:   %d / %d ms\n", min, max
    dur = (lastTs - firstTs) / 1000
    if (dur <= 0) dur = 1
    printf "实际时长:    %.1f s\n", dur
    printf "吞吐量:      %.1f /sec\n", n / dur
}
function pct(p) {
    i = int(n * p / 100 + 0.999999)
    if (i < 1) i = 1
    return elapsed[i]
}
