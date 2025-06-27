# 实验 {序号}：{一句话主题}

> 本文件在跑任何一轮之前填写完成。数据产出后不得回头修改"假设"与"判据"。

## 假设

{做了什么改动，预期产生什么可测量的效果}

## 自变量

- 类型：{配置型 / 代码型}
- 取值：{如 cache.enabled：off（cache-off arm）/ on（cache-on arm）}

## 控制变量

- commit：起点 {hash}，终点 {hash}（配置型则两者相同）
- 负载：threads={}，ramp={}，duration={}
- 数据：ch_post = {} 行；Redis 起点状态：{FLUSHDB / 预热后}
- 环境：{机器、有无其他负载进程}

## 因变量与判据（跑前定死，必须可量化）

- {如：cache-on arm p99 ≤ 10ms}
- {如：cache-on arm MySQL Questions 差分 ≈ 0}
- {如：cache-on arm 修正后命中率 ≥ 80%（hits差分 − 样本数）÷（hits差分 − 样本数 + misses差分）}

## 步骤

1. 每个 arm：warmup 1 轮（-Jduration=30，结果丢弃）
2. 每个 arm：抓基线计数（counters/）→ 3×正式轮（run1~3.jtl）→ 每轮抓终态计数
3. summarize.awk 统计各轮百分位，填入 notes.md
4. 对照判据逐项给出结论，填入 notes.md
