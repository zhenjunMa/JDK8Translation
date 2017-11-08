起因：
    由于之前经常会翻看JDK的源码，尤其是J.U.C包下面的一些类库，发现一些类中开头的大段英文注释写的非常好，
很多时候都说明了设计思路以及这么做的原因，所以想把这些注释翻译成中文，便于自己温习的同时也方便大家。

计划首先从J.U.C包下面的类库开始翻译：

翻译进度汇总：

1.ConcurrentHashMap翻译初版。

2.Striped64翻译初版。该类对于并发计数提出了新的解决思路,ConcurrentHashMap元素个数的统计都是基于该思想做的。

3.翻译线程中的状态类,对应Thread类中的枚举类State。

4.翻译线程池相关类中的部分注释。

5.ClassLoader部分注释翻译。

6.StampedLock部分注释翻译。

7.AbstractQueuedSynchronizer类开头注释翻译，内部类Node部分字段翻译。

8.ScheduledThreadPoolExecutor类部分注释翻译。

9.ScheduledExecutorService类注释。

10.Stream接口部分注释翻译。
