/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package java.util.stream;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;

/**
 * 一个由元素组成的序列，支持顺序并行聚合操作。下面的例子说演示了使用Stream跟IntStream
 * 进行的聚合操作：
 *
 * int sum = widgets.stream()
 *                      .filter(w -> w.getColor() == RED)
 *                      .mapToInt(w -> w.getWeight())
 *                      .sum();
 *
 * 这里，widgets的类型为Collection<Widget>。我们通过Collection.stream()方法创建了一个
 * 由Widget对象组成的流，对它进行过滤，只保留红色的widgets，然后把它转化成一个int类型的流，
 * 其中每个int表示widget的重量。最后把该流中的int相加得到总的重量。
 *
 * 除了表示对象的流Stream，还有很多原始类型的流，比如IntStream，LongStream，DoubleStream
 * 它们统称为stream，并符合此处描述的特征和限制。
 *
 * 执行计算的时候，流操作会组成一个流管道。流管道由一个来源（可以是数组，集合，函数生成器或者IO管道等等），
 * 零个或多个中间操作（它会把当前流变成另一个流，比如Stream#filter），跟一个终止操作（它会产生一个结果或者副作用
 * 比如Stream#count()或者Stream#forEach）。流是很懒的，对于原始数据的计算只有当终止操作初始化时
 * 才会执行，原始元素也只有在需要的时候才会被消费。
 *
 * 集合和流，虽然有一些表面的相似之处，却有不同的目标。集合主要关心的是高效的管理跟访问它们的元素。
 * 相反，流并没有提供直接访问或者操作它们元素的方法，而是声明式描述它们的来源跟在该来源进行聚合操作
 * 时使用的计算方法。如果提供的流操作没有满足需求，可以使用iterator跟spliterator操作来执行自定义
 * 的遍历操作。
 *
 * 流管道，例如上面的widgets例子，可以当成是元数据的查询。除非元数据是专门给并发修改设计的
 * 例如ConcurrentHashMap，当你在查询流上面进行修改的时候可能发成无法预测的错误。
 *
 * 大部分的流操作接受描述用户指定行为的参数，比如上面例子中传给mapToInt方法
 * 的lambda表达式w -> w.getWeight()。为了保持正确的操作，这些操作参数必须遵循以下约定：
 * 1.必须是无干扰的（即不会修改原始流）
 * 2.大多数情况下必须是无状态的（它们产生的结果不能依赖任何在流管道执行过程中可能改变的状态）。
 *
 * 这些参数都是一个functional interface的实例，例如java.util.function.Function，通常
 * 是一个lambda表达式或者方法引用。除非特殊情况不然这些参数都必须是非空的
 *
 * 一个流应该只被操作（执行一个中间或者终止流操作）一次。这个规则适用于，例如fork出来的流，
 * 原始流可能要满足两个或者更多流管道，或者在相同的流上面执行多次遍历。流的实现里，当检测到
 * 该流被复用的时候可能抛出IllegalStateException异常。但是，由于一些流操作可能会返回
 * 它们的接收者而不是新的流对象，并不是所有的情况都可以检测到复用。
 *
 * 流有一个close()方法，实现了AutoCloseable接口，但是几乎所有的流实例在使用之后都不需要
 * 真正的关闭。一般来说，只有元数据是IO管道（例如Files#lines(Path, Charset)返回的）才需要
 * 关闭。大部分流会通过集合，数组，函数生成的，并不需要特殊的资源管理。（如果流需要关闭，它可以
 * 通过try-with-resouce操作声明）。
 *
 * 流管道串行或者并行执行。执行模式是流的一个属性。流可以通过串行或者并行两种方式创建。
 * （例如，Collection.stream()创建一个串行流，Collection.parallelStream()创建一个
 * 并行流）。执行模式可以通过sequential()或者parallel()方法进行修改，还可以通过isParallel()
 * 来查询流模式
 *
 *
 */

/**
 * A sequence of elements supporting sequential and parallel aggregate
 * operations.  The following example illustrates an aggregate operation using
 * {@link Stream} and {@link IntStream}:
 *
 * <pre>{@code
 *     int sum = widgets.stream()
 *                      .filter(w -> w.getColor() == RED)
 *                      .mapToInt(w -> w.getWeight())
 *                      .sum();
 * }</pre>
 *
 * In this example, {@code widgets} is a {@code Collection<Widget>}.  We create
 * a stream of {@code Widget} objects via {@link Collection#stream Collection.stream()},
 * filter it to produce a stream containing only the red widgets, and then
 * transform it into a stream of {@code int} values representing the weight of
 * each red widget. Then this stream is summed to produce a total weight.
 *
 * <p>In addition to {@code Stream}, which is a stream of object references,
 * there are primitive specializations for {@link IntStream}, {@link LongStream},
 * and {@link DoubleStream}, all of which are referred to as "streams" and
 * conform to the characteristics and restrictions described here.
 *
 * <p>To perform a computation, stream
 * <a href="package-summary.html#StreamOps">operations</a> are composed into a
 * <em>stream pipeline</em>.  A stream pipeline consists of a source (which
 * might be an array, a collection, a generator function, an I/O channel,
 * etc), zero or more <em>intermediate operations</em> (which transform a
 * stream into another stream, such as {@link Stream#filter(Predicate)}), and a
 * <em>terminal operation</em> (which produces a result or side-effect, such
 * as {@link Stream#count()} or {@link Stream#forEach(Consumer)}).
 * Streams are lazy; computation on the source data is only performed when the
 * terminal operation is initiated, and source elements are consumed only
 * as needed.
 *
 * <p>Collections and streams, while bearing some superficial similarities,
 * have different goals.  Collections are primarily concerned with the efficient
 * management of, and access to, their elements.  By contrast, streams do not
 * provide a means to directly access or manipulate their elements, and are
 * instead concerned with declaratively describing their source and the
 * computational operations which will be performed in aggregate on that source.
 * However, if the provided stream operations do not offer the desired
 * functionality, the {@link #iterator()} and {@link #spliterator()} operations
 * can be used to perform a controlled traversal.
 *
 * <p>A stream pipeline, like the "widgets" example above, can be viewed as
 * a <em>query</em> on the stream source.  Unless the source was explicitly
 * designed for concurrent modification (such as a {@link ConcurrentHashMap}),
 * unpredictable or erroneous behavior may result from modifying the stream
 * source while it is being queried.
 *
 * <p>Most stream operations accept parameters that describe user-specified
 * behavior, such as the lambda expression {@code w -> w.getWeight()} passed to
 * {@code mapToInt} in the example above.  To preserve correct behavior,
 * these <em>behavioral parameters</em>:
 * <ul>
 * <li>must be <a href="package-summary.html#NonInterference">non-interfering</a>
 * (they do not modify the stream source); and</li>
 * <li>in most cases must be <a href="package-summary.html#Statelessness">stateless</a>
 * (their result should not depend on any state that might change during execution
 * of the stream pipeline).</li>
 * </ul>
 *
 * <p>Such parameters are always instances of a
 * <a href="../function/package-summary.html">functional interface</a> such
 * as {@link java.util.function.Function}, and are often lambda expressions or
 * method references.  Unless otherwise specified these parameters must be
 * <em>non-null</em>.
 *
 * <p>A stream should be operated on (invoking an intermediate or terminal stream
 * operation) only once.  This rules out, for example, "forked" streams, where
 * the same source feeds two or more pipelines, or multiple traversals of the
 * same stream.  A stream implementation may throw {@link IllegalStateException}
 * if it detects that the stream is being reused. However, since some stream
 * operations may return their receiver rather than a new stream object, it may
 * not be possible to detect reuse in all cases.
 *
 * <p>Streams have a {@link #close()} method and implement {@link AutoCloseable},
 * but nearly all stream instances do not actually need to be closed after use.
 * Generally, only streams whose source is an IO channel (such as those returned
 * by {@link Files#lines(Path, Charset)}) will require closing.  Most streams
 * are backed by collections, arrays, or generating functions, which require no
 * special resource management.  (If a stream does require closing, it can be
 * declared as a resource in a {@code try}-with-resources statement.)
 *
 * <p>Stream pipelines may execute either sequentially or in
 * <a href="package-summary.html#Parallelism">parallel</a>.  This
 * execution mode is a property of the stream.  Streams are created
 * with an initial choice of sequential or parallel execution.  (For example,
 * {@link Collection#stream() Collection.stream()} creates a sequential stream,
 * and {@link Collection#parallelStream() Collection.parallelStream()} creates
 * a parallel one.)  This choice of execution mode may be modified by the
 * {@link #sequential()} or {@link #parallel()} methods, and may be queried with
 * the {@link #isParallel()} method.
 *
 * @param <T> the type of the stream elements
 * @since 1.8
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see <a href="package-summary.html">java.util.stream</a>
 */
public interface Stream<T> extends BaseStream<T, Stream<T>> {

    /**
     * 返回当前流中满足传入参数predicate条件的元素组成的新流。
     * 这是一个中间操作。
     *
     * Returns a stream consisting of the elements of this stream that match
     * the given predicate.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to each element to determine if it
     *                  should be included
     * @return the new stream
     */
    Stream<T> filter(Predicate<? super T> predicate);

    /**
     * 返回对当前流中每个元素使用传入函数生成新元素组成的新流。
     * 这是一个中间操作。
     *
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    /**
     * 返回对当前流中每个元素使用传入函数生成的int类型元素组成的新流。
     * 这是一个中间操作
     *
     * Returns an {@code IntStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">
     *     intermediate operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    IntStream mapToInt(ToIntFunction<? super T> mapper);

    /**
     * 返回对当前流中每个元素使用传入函数生成的long类型元素组成的新流。
     * 这是一个中间操作
     *
     * Returns a {@code LongStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    LongStream mapToLong(ToLongFunction<? super T> mapper);

    /**
     * 返回对当前流中每个元素使用传入函数生成的double类型元素组成的新流。
     * 这是一个中间操作
     *
     * Returns a {@code DoubleStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    /**
     * 返回一个由当前流中的所有元素使用参数传入的函数转换成的子流再组合成一个完整的新流的
     * 流。该方法的意思是传入的函数会把当前流中的每个元素都转换成一个对应的流，然后在把这
     * 些对应的流中的内容统一放在一个新流中返回。
     * 注：每个传入函数生成的子流都是java.util.stream.BaseStream类型，当其中的元素都
     * 加入到新流以后会调用里面的close()方法进行关闭，如果子流为null，则会用一个空的流代替。
     *
     * 这是一个中间操作。
     *
     * 注：flatMap()方法用于把一个元素映射成含有多个元素的流，然后再把这些流组合成一个
     * 完成的流的场景。
     *
     * 举例1：
     * 现在假设orders是一个采购订单流，每个采购订单又包含了一系列的商品，下面的代码会生成
     * 一个包含所有采购订单中所有商品的流。
     * orders.flatMap(order -> order.getLineItems().stream())
     *
     * 举例2：
     * 假设path表示一个文件的路径，下面的代码会生成一个包含文件中所有单词的流。
     * Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8);
     * Stream<String> words = lines.flatMap(line -> Stream.of(line.split(" +")));
     * 传给flatMap的函数使用了一个简单的表达式把一行分成一个数组，然后再基于该数组创建一个流。
     *
     *
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.  Each mapped stream is
     * {@link java.util.stream.BaseStream#close() closed} after its contents
     * have been placed into this stream.  (If a mapped stream is {@code null}
     * an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @apiNote
     * The {@code flatMap()} operation has the effect of applying a one-to-many
     * transformation to the elements of the stream, and then flattening the
     * resulting elements into a new stream.
     *
     * <p><b>Examples.</b>
     *
     * <p>If {@code orders} is a stream of purchase orders, and each purchase
     * order contains a collection of line items, then the following produces a
     * stream containing all the line items in all the orders:
     * <pre>{@code
     *     orders.flatMap(order -> order.getLineItems().stream())...
     * }</pre>
     *
     * <p>If {@code path} is the path to a file, then the following produces a
     * stream of the {@code words} contained in that file:
     * <pre>{@code
     *     Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8);
     *     Stream<String> words = lines.flatMap(line -> Stream.of(line.split(" +")));
     * }</pre>
     * The {@code mapper} function passed to {@code flatMap} splits a line,
     * using a simple regular expression, into an array of words, and then
     * creates a stream of words from that array.
     *
     * @param <R> The element type of the new stream
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     * @return the new stream
     */
    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    /**
     * 详情参考flatMap方法注释，只是该方法指定了传入函数必须把原来流中的数据转换成一个
     * int类型的流才行。
     * 这是一个中间操作。
     *
     * Returns an {@code IntStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.BaseStream#close() closed} after its
     * contents have been placed into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     * @return the new stream
     * @see #flatMap(Function)
     */
    IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

    /**
     * 详情参考flatMap方法注释，只是该方法指定了传入函数必须把原来流中的数据转换成一个
     * long类型的流才行。
     * 这是一个中间操作。
     *
     *
     * Returns an {@code LongStream} consisting of the results of replacing each
     * element of this stream with the contents of a mapped stream produced by
     * applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.BaseStream#close() closed} after its
     * contents have been placed into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     * @return the new stream
     * @see #flatMap(Function)
     */
    LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);

    /**
     * 详情参考flatMap方法注释，只是该方法指定了传入函数必须把原来流中的数据转换成一个
     * double类型的流才行。
     * 这是一个中间操作。
     *
     * Returns an {@code DoubleStream} consisting of the results of replacing
     * each element of this stream with the contents of a mapped stream produced
     * by applying the provided mapping function to each element.  Each mapped
     * stream is {@link java.util.stream.BaseStream#close() closed} after its
     * contents have placed been into this stream.  (If a mapped stream is
     * {@code null} an empty stream is used, instead.)
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces a stream
     *               of new values
     * @return the new stream
     * @see #flatMap(Function)
     */
    DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);

    /**
     * 返回一个由当前流中不同元素组成的新流，使用equals()方法进行判断。
     *
     * 对于一个已经排好序的流来说，选择不同元素时稳定的（对于重复的元素，会保留第一个出现的元素）。
     * 对于一个未排序的流来说，没有任何稳定的保证。
     *
     * 这是一个中间操作。
     *
     * 注：
     * distinct()方法要想在并行操作中保持稳定性是需要付出更多代价的，（需要消耗大量的缓存，
     * 把该操作作为一个完整的屏障）而且这种稳定性通常又是不需要的。使用一个未排序的原始流
     * （例如使用generate(Supplier)方法生成）或者使用unordered()方法删除排序约束可以提高
     * distinct()方法在并行管道中的执行效率，前提是你允许这种情况。
     * 如果想要保证一致性，而且你又面临distinct()在并行管道中带来的低性能，低内存利用率的问题，
     * 那么使用串行执行的sequential()方法可能会提高性能。
     *
     * Returns a stream consisting of the distinct elements (according to
     * {@link Object#equals(Object)}) of this stream.
     *
     * <p>For ordered streams, the selection of distinct elements is stable
     * (for duplicated elements, the element appearing first in the encounter
     * order is preserved.)  For unordered streams, no stability guarantees
     * are made.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @apiNote
     * Preserving stability for {@code distinct()} in parallel pipelines is
     * relatively expensive (requires that the operation act as a full barrier,
     * with substantial buffering overhead), and stability is often not needed.
     * Using an unordered stream source (such as {@link #generate(Supplier)})
     * or removing the ordering constraint with {@link #unordered()} may result
     * in significantly more efficient execution for {@code distinct()} in parallel
     * pipelines, if the semantics of your situation permit.  If consistency
     * with encounter order is required, and you are experiencing poor performance
     * or memory utilization with {@code distinct()} in parallel pipelines,
     * switching to sequential execution with {@link #sequential()} may improve
     * performance.
     *
     * @return the new stream
     */
    Stream<T> distinct();

    /**
     * 返回当前流中元素进行自然排序后组成的新流。如果该流中的元素不是Comparable类型，
     * 那么在流的终止操作中会抛出ClassCastException异常。
     * 对于已经排序的流来说，这个排序是稳定的。对于未排序的流来说，没有任何稳定保证。
     *
     * 这是一个中间操作。
     *
     *
     * Returns a stream consisting of the elements of this stream, sorted
     * according to natural order.  If the elements of this stream are not
     * {@code Comparable}, a {@code java.lang.ClassCastException} may be thrown
     * when the terminal operation is executed.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @return the new stream
     */
    Stream<T> sorted();

    /**
     * 返回一个由当前流中元素使用传入的Comparator进行排序的新流。
     * 对于已经排序的流来说，这个排序是稳定的。对于未排序的流来说，没有任何稳定保证。
     *
     * 这是一个中间操作。
     *
     * Returns a stream consisting of the elements of this stream, sorted
     * according to the provided {@code Comparator}.
     *
     * <p>For ordered streams, the sort is stable.  For unordered streams, no
     * stability guarantees are made.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to be used to compare stream elements
     * @return the new stream
     */
    Stream<T> sorted(Comparator<? super T> comparator);

    /**
     * 返回一个由原流中元素组成的流，另外在新流中的元素被消费时对每个元素使用传入的函数
     * 进行处理。
     *
     * 这是一个中间操作。
     *
     * 对于并行流管道来说，入参函数可能在任何时间被任何线程被调用。如果该函数修改了共享状态，
     * 那么它需要自己保证并发控制。
     *
     * 注：
     * 该方法的存在主要用来支持debug，当你想要查看元素通过一个流管道之后的状态时使用。
     * 例如：
     * Stream.of("one", "two", "three", "four")
     *         .filter(e -> e.length() > 3)
     *         .peek(e -> System.out.println("Filtered value: " + e))
     *         .map(String::toUpperCase)
     *         .peek(e -> System.out.println("Mapped value: " + e))
     *         .collect(Collectors.toList());
     *
     *
     * Returns a stream consisting of the elements of this stream, additionally
     * performing the provided action on each element as elements are consumed
     * from the resulting stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * <p>For parallel stream pipelines, the action may be called at
     * whatever time and in whatever thread the element is made available by the
     * upstream operation.  If the action modifies shared state,
     * it is responsible for providing the required synchronization.
     *
     * @apiNote This method exists mainly to support debugging, where you want
     * to see the elements as they flow past a certain point in a pipeline:
     * <pre>{@code
     *     Stream.of("one", "two", "three", "four")
     *         .filter(e -> e.length() > 3)
     *         .peek(e -> System.out.println("Filtered value: " + e))
     *         .map(String::toUpperCase)
     *         .peek(e -> System.out.println("Mapped value: " + e))
     *         .collect(Collectors.toList());
     * }</pre>
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *                 non-interfering</a> action to perform on the elements as
     *                 they are consumed from the stream
     * @return the new stream
     */
    Stream<T> peek(Consumer<? super T> action);

    /**
     * 返回由当前流中数量小于等于maxSize的元素组成的流。
     *
     * 这是一个短暂的中间操作。
     *
     * 注：
     * 尽管limit()方法对于串行流管道来说是一个廉价的操作，但由于limit(n)并不是返回当前流中
     * 任意的n个元素，而是当前流中前n个元素，这就在并行流管道中，尤其是当maxSize很大是需要
     * 很大代价。使用一个未排序的原始流（例如使用generate(Supplier)方法生成）或者使用unordered()
     * 方法删除排序约束可以提高limit()方法在并行管道中的执行效率，前提是你允许这种情况。
     * 如果想要保证一致性，而且你又面临limit()在并行管道中带来的低性能，低内存利用率的问题，
     * 那么使用串行执行的sequential()方法可能会提高性能。
     *
     * Returns a stream consisting of the elements of this stream, truncated
     * to be no longer than {@code maxSize} in length.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * stateful intermediate operation</a>.
     *
     * @apiNote
     * While {@code limit()} is generally a cheap operation on sequential
     * stream pipelines, it can be quite expensive on ordered parallel pipelines,
     * especially for large values of {@code maxSize}, since {@code limit(n)}
     * is constrained to return not just any <em>n</em> elements, but the
     * <em>first n</em> elements in the encounter order.  Using an unordered
     * stream source (such as {@link #generate(Supplier)}) or removing the
     * ordering constraint with {@link #unordered()} may result in significant
     * speedups of {@code limit()} in parallel pipelines, if the semantics of
     * your situation permit.  If consistency with encounter order is required,
     * and you are experiencing poor performance or memory utilization with
     * {@code limit()} in parallel pipelines, switching to sequential execution
     * with {@link #sequential()} may improve performance.
     *
     * @param maxSize the number of elements the stream should be limited to
     * @return the new stream
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    Stream<T> limit(long maxSize);

    /**
     * 返回一个由当前流中跳过前n个元素，剩余元素组成的新流。
     * 如果当前流中的元素小于n个，则返回一个空的流。
     *
     * 这是一个有状态的中间操作。
     *
     * 注：
     * 尽管skip()方法对于串行流管道来说是一个廉价的操作，但由于skip(n)并不是跳过当前流中
     * 任意的n个元素，而是当前流中前n个元素，这就在并行流管道中，尤其是当n很大是需要
     * 很大代价。使用一个未排序的原始流（例如使用generate(Supplier)方法生成）或者使用unordered()
     * 方法删除排序约束可以提高skip()方法在并行管道中的执行效率，前提是你允许这种情况。
     * 如果想要保证一致性，而且你又面临skip()在并行管道中带来的低性能，低内存利用率的问题，
     * 那么使用串行执行的sequential()方法可能会提高性能。
     *
     * Returns a stream consisting of the remaining elements of this stream
     * after discarding the first {@code n} elements of the stream.
     * If this stream contains fewer than {@code n} elements then an
     * empty stream will be returned.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">stateful
     * intermediate operation</a>.
     *
     * @apiNote
     * While {@code skip()} is generally a cheap operation on sequential
     * stream pipelines, it can be quite expensive on ordered parallel pipelines,
     * especially for large values of {@code n}, since {@code skip(n)}
     * is constrained to skip not just any <em>n</em> elements, but the
     * <em>first n</em> elements in the encounter order.  Using an unordered
     * stream source (such as {@link #generate(Supplier)}) or removing the
     * ordering constraint with {@link #unordered()} may result in significant
     * speedups of {@code skip()} in parallel pipelines, if the semantics of
     * your situation permit.  If consistency with encounter order is required,
     * and you are experiencing poor performance or memory utilization with
     * {@code skip()} in parallel pipelines, switching to sequential execution
     * with {@link #sequential()} may improve performance.
     *
     * @param n the number of leading elements to skip
     * @return the new stream
     * @throws IllegalArgumentException if {@code n} is negative
     */
    Stream<T> skip(long n);

    /**
     * 对当前流中的每个元素都执行一次入参函数。
     *
     * 这是一个终止操作。
     *
     * 该操作是明确不确定的。对于并行流来说，它不保证操作的元素的顺序，因为这样
     * 会牺牲并行的性能。对于给定的元素，该入参函数会在任意时间任何线程中执行，
     * 如果入参函数需要操作共享状态，那么需要它自己保证并发控制。
     *
     * Performs an action for each element of this stream.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * <p>The behavior of this operation is explicitly nondeterministic.
     * For parallel stream pipelines, this operation does <em>not</em>
     * guarantee to respect the encounter order of the stream, as doing so
     * would sacrifice the benefit of parallelism.  For any given element, the
     * action may be performed at whatever time and in whatever thread the
     * library chooses.  If the action accesses shared state, it is
     * responsible for providing the required synchronization.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *               non-interfering</a> action to perform on the elements
     */
    void forEach(Consumer<? super T> action);

    /**
     * 该方法与forEach的区别是在并行流中，它依然能够保持按顺序处理元素。
     *
     * 这是一个终止操作。
     *
     * 对于一个元素执行action操作happens-befor于对该元素的后续元素执行action，
     * 但对于任意给定元素，执行的线程是不确定的。
     *
     * Performs an action for each element of this stream, in the encounter
     * order of the stream if the stream has a defined encounter order.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * <p>This operation processes the elements one at a time, in encounter
     * order if one exists.  Performing the action for one element
     * <a href="../concurrent/package-summary.html#MemoryVisibility"><i>happens-before</i></a>
     * performing the action for subsequent elements, but for any given element,
     * the action may be performed in whatever thread the library chooses.
     *
     * @param action a <a href="package-summary.html#NonInterference">
     *               non-interfering</a> action to perform on the elements
     * @see #forEach(Consumer)
     */
    void forEachOrdered(Consumer<? super T> action);

    /**
     * 返回一个包含该流中所有元素的数组。
     *
     * 这是一个终止操作。
     *
     *
     * Returns an array containing the elements of this stream.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @return an array containing the elements of this stream
     */
    Object[] toArray();

    /**
     * 返回一个包含流中所有元素的数组，使用提供的generator函数分配返回的数组，以及做一些
     * 额外操作，比如分片执行或者resizing。
     *
     * 这是一个终止操作。
     *
     * 注：
     * generator函数需要一个表示数组的大小int参数，返回一个指定大小的数组，可以通过以下
     * 方式进行简写：
     * Person[] men = people.stream()
     *                          .filter(p -> p.getGender() == MALE)
     *                          .toArray(Person[]::new);
     *
     *
     * Returns an array containing the elements of this stream, using the
     * provided {@code generator} function to allocate the returned array, as
     * well as any additional arrays that might be required for a partitioned
     * execution or for resizing.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote
     * The generator function takes an integer, which is the size of the
     * desired array, and produces an array of the desired size.  This can be
     * concisely expressed with an array constructor reference:
     * <pre>{@code
     *     Person[] men = people.stream()
     *                          .filter(p -> p.getGender() == MALE)
     *                          .toArray(Person[]::new);
     * }</pre>
     *
     * @param <A> the element type of the resulting array
     * @param generator a function which produces a new array of the desired
     *                  type and the provided length
     * @return an array containing the elements in this stream
     * @throws ArrayStoreException if the runtime type of the array returned
     * from the array generator is not a supertype of the runtime type of every
     * element in this stream
     */
    <A> A[] toArray(IntFunction<A[]> generator);

    /**
     * 使用提供的identity值以及一个累加器函数对流中元素执行一个削减操作，返回一个削减以后的值。
     * 类似于如下操作：
     *     T result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * 但是不保证顺序处理每个元素。
     *
     * 参数identity必须是accumulator函数的identity，也就是说对于所有的t， accumulator.apply(identity, t)
     * 都等于t，函数accumulator必须是个联想函数。
     *
     * 这是一个终止操作。
     *
     * 注：
     * 求和, 取小, 取大, 取平均数跟拼接字符串都是削减操作的特殊案例。对数字组成的流求和
     * 可以用以下方式：
     * Integer sum = integers.reduce(0, (a, b) -> a+b);
     * 或者：
     * Integer sum = integers.reduce(0, Integer::sum);
     *
     * 尽管与直接在循环中进行计算相比，这种方式算是一种迂回的聚合操作。但这种削减操作可以
     * 让更优雅的进行并行执行，有效的减少额外的并发控制以及竞争。
     *
     *
     *
     *
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using the provided identity value and an
     * <a href="package-summary.html#Associativity">associative</a>
     * accumulation function, and returns the reduced value.  This is equivalent
     * to:
     * <pre>{@code
     *     T result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code t},
     * {@code accumulator.apply(identity, t)} is equal to {@code t}.
     * The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote Sum, min, max, average, and string concatenation are all special
     * cases of reduction. Summing a stream of numbers can be expressed as:
     *
     * <pre>{@code
     *     Integer sum = integers.reduce(0, (a, b) -> a+b);
     * }</pre>
     *
     * or:
     *
     * <pre>{@code
     *     Integer sum = integers.reduce(0, Integer::sum);
     * }</pre>
     *
     * <p>While this may seem a more roundabout way to perform an aggregation
     * compared to simply mutating a running total in a loop, reduction
     * operations parallelize more gracefully, without needing additional
     * synchronization and with greatly reduced risk of data races.
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values
     * @return the result of the reduction
     */
    T reduce(T identity, BinaryOperator<T> accumulator);

    /**
     * 用一个联想累计函数对流中的元素执行一个削减操作，如果有的话，返回一个由Optional封装好的值。
     * 该操作等同于：
     * boolean foundAny = false;
     *     T result = null;
     *     for (T element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.apply(result, element);
     *     }
     *     return foundAny ? Optional.of(result) : Optional.empty();
     * 但并不是严格执行的。
     *
     * accumulator函数必须是一个联想函数。
     *
     * 这是一个终止操作。
     *
     *
     *
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using an
     * <a href="package-summary.html#Associativity">associative</a> accumulation
     * function, and returns an {@code Optional} describing the reduced value,
     * if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     T result = null;
     *     for (T element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.apply(result, element);
     *     }
     *     return foundAny ? Optional.of(result) : Optional.empty();
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values
     * @return an {@link Optional} describing the result of the reduction
     * @throws NullPointerException if the result of the reduction is null
     * @see #reduce(Object, BinaryOperator)
     * @see #min(Comparator)
     * @see #max(Comparator)
     */
    Optional<T> reduce(BinaryOperator<T> accumulator);

    /**
     * 使用提供的identity，叠加跟合并函数对流中的元素执行一个削减操作，这等效于：
     *
     * U result = identity;
     * for (T element : this stream)
     *    result = accumulator.apply(result, element)
     * return result;
     *
     * 但不是严格按顺序执行。
     *
     * identity的值必须满足combiner函数。意思是对于所有的类型u，combiner(identity, u)
     * 于u相同。此外，combiner函数必须与accumulator函数一致；对于所有的u跟t,必须满足以下
     * 条件：
     * combiner.apply(u, accumulator.apply(identity, t)) == accumulator.apply(u, t)
     *
     * 这是一个终止操作。
     *
     * 注：
     * 很多使用这种方式的削减操作都可以更简单的使用map跟reduce操作组合实现。accumulator函数
     * 融合了mapper跟accumulator，很多时候可以比分开的mapping跟reduction更加高效，
     * 比如已知了之前的一些削减操作可以让你减少一些计算。
     *
     *
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using the provided identity, accumulation and
     * combining functions.  This is equivalent to:
     * <pre>{@code
     *     U result = identity;
     *     for (T element : this stream)
     *         result = accumulator.apply(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code identity} value must be an identity for the combiner
     * function.  This means that for all {@code u}, {@code combiner(identity, u)}
     * is equal to {@code u}.  Additionally, the {@code combiner} function
     * must be compatible with the {@code accumulator} function; for all
     * {@code u} and {@code t}, the following must hold:
     * <pre>{@code
     *     combiner.apply(u, accumulator.apply(identity, t)) == accumulator.apply(u, t)
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote Many reductions using this form can be represented more simply
     * by an explicit combination of {@code map} and {@code reduce} operations.
     * The {@code accumulator} function acts as a fused mapper and accumulator,
     * which can sometimes be more efficient than separate mapping and reduction,
     * such as when knowing the previously reduced value allows you to avoid
     * some computation.
     *
     * @param <U> The type of the result
     * @param identity the identity value for the combiner function
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for incorporating an additional element into a result
     * @param combiner an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     * @return the result of the reduction
     * @see #reduce(BinaryOperator)
     * @see #reduce(Object, BinaryOperator)
     */
    <U> U reduce(U identity,
                 BiFunction<U, ? super T, U> accumulator,
                 BinaryOperator<U> combiner);

    /**
     * Performs a <a href="package-summary.html#MutableReduction">mutable
     * reduction</a> operation on the elements of this stream.  A mutable
     * reduction is one in which the reduced value is a mutable result container,
     * such as an {@code ArrayList}, and elements are incorporated by updating
     * the state of the result rather than by replacing the result.  This
     * produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (T element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     *
     * <p>Like {@link #reduce(Object, BinaryOperator)}, {@code collect} operations
     * can be parallelized without requiring additional synchronization.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote There are many existing classes in the JDK whose signatures are
     * well-suited for use with method references as arguments to {@code collect()}.
     * For example, the following will accumulate strings into an {@code ArrayList}:
     * <pre>{@code
     *     List<String> asList = stringStream.collect(ArrayList::new, ArrayList::add,
     *                                                ArrayList::addAll);
     * }</pre>
     *
     * <p>The following will take a stream of strings and concatenates them into a
     * single string:
     * <pre>{@code
     *     String concat = stringStream.collect(StringBuilder::new, StringBuilder::append,
     *                                          StringBuilder::append)
     *                                 .toString();
     * }</pre>
     *
     * @param <R> type of the result
     * @param supplier a function that creates a new result container. For a
     *                 parallel execution, this function may be called
     *                 multiple times and must return a fresh value each time.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for incorporating an additional element into a result
     * @param combiner an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     * @return the result of the reduction
     */
    <R> R collect(Supplier<R> supplier,
                  BiConsumer<R, ? super T> accumulator,
                  BiConsumer<R, R> combiner);

    /**
     * Performs a <a href="package-summary.html#MutableReduction">mutable
     * reduction</a> operation on the elements of this stream using a
     * {@code Collector}.  A {@code Collector}
     * encapsulates the functions used as arguments to
     * {@link #collect(Supplier, BiConsumer, BiConsumer)}, allowing for reuse of
     * collection strategies and composition of collect operations such as
     * multiple-level grouping or partitioning.
     *
     * <p>If the stream is parallel, and the {@code Collector}
     * is {@link Collector.Characteristics#CONCURRENT concurrent}, and
     * either the stream is unordered or the collector is
     * {@link Collector.Characteristics#UNORDERED unordered},
     * then a concurrent reduction will be performed (see {@link Collector} for
     * details on concurrent reduction.)
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * <p>When executed in parallel, multiple intermediate results may be
     * instantiated, populated, and merged so as to maintain isolation of
     * mutable data structures.  Therefore, even when executed in parallel
     * with non-thread-safe data structures (such as {@code ArrayList}), no
     * additional synchronization is needed for a parallel reduction.
     *
     * @apiNote
     * The following will accumulate strings into an ArrayList:
     * <pre>{@code
     *     List<String> asList = stringStream.collect(Collectors.toList());
     * }</pre>
     *
     * <p>The following will classify {@code Person} objects by city:
     * <pre>{@code
     *     Map<String, List<Person>> peopleByCity
     *         = personStream.collect(Collectors.groupingBy(Person::getCity));
     * }</pre>
     *
     * <p>The following will classify {@code Person} objects by state and city,
     * cascading two {@code Collector}s together:
     * <pre>{@code
     *     Map<String, Map<String, List<Person>>> peopleByStateAndCity
     *         = personStream.collect(Collectors.groupingBy(Person::getState,
     *                                                      Collectors.groupingBy(Person::getCity)));
     * }</pre>
     *
     * @param <R> the type of the result
     * @param <A> the intermediate accumulation type of the {@code Collector}
     * @param collector the {@code Collector} describing the reduction
     * @return the result of the reduction
     * @see #collect(Supplier, BiConsumer, BiConsumer)
     * @see Collectors
     */
    <R, A> R collect(Collector<? super T, A, R> collector);

    /**
     * Returns the minimum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a
     * <a href="package-summary.html#Reduction">reduction</a>.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to compare elements of this stream
     * @return an {@code Optional} describing the minimum element of this stream,
     * or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the minimum element is null
     */
    Optional<T> min(Comparator<? super T> comparator);

    /**
     * Returns the maximum element of this stream according to the provided
     * {@code Comparator}.  This is a special case of a
     * <a href="package-summary.html#Reduction">reduction</a>.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param comparator a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                   <a href="package-summary.html#Statelessness">stateless</a>
     *                   {@code Comparator} to compare elements of this stream
     * @return an {@code Optional} describing the maximum element of this stream,
     * or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the maximum element is null
     */
    Optional<T> max(Comparator<? super T> comparator);

    /**
     * Returns the count of elements in this stream.  This is a special case of
     * a <a href="package-summary.html#Reduction">reduction</a> and is
     * equivalent to:
     * <pre>{@code
     *     return mapToLong(e -> 1L).sum();
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal operation</a>.
     *
     * @return the count of elements in this stream
     */
    long count();

    /**
     * Returns whether any elements of this stream match the provided
     * predicate.  May not evaluate the predicate on all elements if not
     * necessary for determining the result.  If the stream is empty then
     * {@code false} is returned and the predicate is not evaluated.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * @apiNote
     * This method evaluates the <em>existential quantification</em> of the
     * predicate over the elements of the stream (for some x P(x)).
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements of this stream
     * @return {@code true} if any elements of the stream match the provided
     * predicate, otherwise {@code false}
     */
    boolean anyMatch(Predicate<? super T> predicate);

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.  If the stream is empty then {@code true} is
     * returned and the predicate is not evaluated.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * @apiNote
     * This method evaluates the <em>universal quantification</em> of the
     * predicate over the elements of the stream (for all x P(x)).  If the
     * stream is empty, the quantification is said to be <em>vacuously
     * satisfied</em> and is always {@code true} (regardless of P(x)).
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements of this stream
     * @return {@code true} if either all elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    boolean allMatch(Predicate<? super T> predicate);

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result.  If the stream is empty then {@code true} is
     * returned and the predicate is not evaluated.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * @apiNote
     * This method evaluates the <em>universal quantification</em> of the
     * negated predicate over the elements of the stream (for all x ~P(x)).  If
     * the stream is empty, the quantification is said to be vacuously satisfied
     * and is always {@code true}, regardless of P(x).
     *
     * @param predicate a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *                  <a href="package-summary.html#Statelessness">stateless</a>
     *                  predicate to apply to elements of this stream
     * @return {@code true} if either no elements of the stream match the
     * provided predicate or the stream is empty, otherwise {@code false}
     */
    boolean noneMatch(Predicate<? super T> predicate);

    /**
     * Returns an {@link Optional} describing the first element of this stream,
     * or an empty {@code Optional} if the stream is empty.  If the stream has
     * no encounter order, then any element may be returned.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * @return an {@code Optional} describing the first element of this stream,
     * or an empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the element selected is null
     */
    Optional<T> findFirst();

    /**
     * Returns an {@link Optional} describing some element of the stream, or an
     * empty {@code Optional} if the stream is empty.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">short-circuiting
     * terminal operation</a>.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is
     * free to select any element in the stream.  This is to allow for maximal
     * performance in parallel operations; the cost is that multiple invocations
     * on the same source may not return the same result.  (If a stable result
     * is desired, use {@link #findFirst()} instead.)
     *
     * @return an {@code Optional} describing some element of this stream, or an
     * empty {@code Optional} if the stream is empty
     * @throws NullPointerException if the element selected is null
     * @see #findFirst()
     */
    Optional<T> findAny();

    // Static factories

    /**
     * Returns a builder for a {@code Stream}.
     *
     * @param <T> type of elements
     * @return a stream builder
     */
    public static<T> Builder<T> builder() {
        return new Streams.StreamBuilderImpl<>();
    }

    /**
     * 返回一个空的顺序流。
     *
     * Returns an empty sequential {@code Stream}.
     *
     * @param <T> the type of stream elements
     * @return an empty sequential stream
     */
    public static<T> Stream<T> empty() {
        return StreamSupport.stream(Spliterators.<T>emptySpliterator(), false);
    }

    /**
     * 返回一个仅含有给定的一个元素组成的顺序流。
     *
     * Returns a sequential {@code Stream} containing a single element.
     *
     * @param t the single element
     * @param <T> the type of stream elements
     * @return a singleton sequential stream
     */
    public static<T> Stream<T> of(T t) {
        return StreamSupport.stream(new Streams.StreamBuilderImpl<>(t), false);
    }

    /**
     *
     * 返回一个由给定元素进行顺序排列组成的流。
     *
     * Returns a sequential ordered stream whose elements are the specified values.
     *
     * @param <T> the type of stream elements
     * @param values the elements of the new stream
     * @return the new stream
     */
    @SafeVarargs
    @SuppressWarnings("varargs") // Creating a stream from an array is safe
    public static<T> Stream<T> of(T... values) {
        return Arrays.stream(values);
    }

    /**
     * Returns an infinite sequential ordered {@code Stream} produced by iterative
     * application of a function {@code f} to an initial element {@code seed},
     * producing a {@code Stream} consisting of {@code seed}, {@code f(seed)},
     * {@code f(f(seed))}, etc.
     *
     * <p>The first element (position {@code 0}) in the {@code Stream} will be
     * the provided {@code seed}.  For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     *
     * @param <T> the type of stream elements
     * @param seed the initial element
     * @param f a function to be applied to to the previous element to produce
     *          a new element
     * @return a new sequential {@code Stream}
     */
    public static<T> Stream<T> iterate(final T seed, final UnaryOperator<T> f) {
        Objects.requireNonNull(f);
        final Iterator<T> iterator = new Iterator<T>() {
            @SuppressWarnings("unchecked")
            T t = (T) Streams.NONE;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return t = (t == Streams.NONE) ? seed : f.apply(t);
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iterator,
                Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }

    /**
     * Returns an infinite sequential unordered stream where each element is
     * generated by the provided {@code Supplier}.  This is suitable for
     * generating constant streams, streams of random elements, etc.
     *
     * @param <T> the type of stream elements
     * @param s the {@code Supplier} of generated elements
     * @return a new infinite sequential unordered {@code Stream}
     */
    public static<T> Stream<T> generate(Supplier<T> s) {
        Objects.requireNonNull(s);
        return StreamSupport.stream(
                new StreamSpliterators.InfiniteSupplyingSpliterator.OfRef<>(Long.MAX_VALUE, s), false);
    }

    /**
     * Creates a lazily concatenated stream whose elements are all the
     * elements of the first stream followed by all the elements of the
     * second stream.  The resulting stream is ordered if both
     * of the input streams are ordered, and parallel if either of the input
     * streams is parallel.  When the resulting stream is closed, the close
     * handlers for both input streams are invoked.
     *
     * @implNote
     * Use caution when constructing streams from repeated concatenation.
     * Accessing an element of a deeply concatenated stream can result in deep
     * call chains, or even {@code StackOverflowException}.
     *
     * @param <T> The type of stream elements
     * @param a the first stream
     * @param b the second stream
     * @return the concatenation of the two input streams
     */
    public static <T> Stream<T> concat(Stream<? extends T> a, Stream<? extends T> b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        @SuppressWarnings("unchecked")
        Spliterator<T> split = new Streams.ConcatSpliterator.OfRef<>(
                (Spliterator<T>) a.spliterator(), (Spliterator<T>) b.spliterator());
        Stream<T> stream = StreamSupport.stream(split, a.isParallel() || b.isParallel());
        return stream.onClose(Streams.composedClose(a, b));
    }

    /**
     * A mutable builder for a {@code Stream}.  This allows the creation of a
     * {@code Stream} by generating elements individually and adding them to the
     * {@code Builder} (without the copying overhead that comes from using
     * an {@code ArrayList} as a temporary buffer.)
     *
     * <p>A stream builder has a lifecycle, which starts in a building
     * phase, during which elements can be added, and then transitions to a built
     * phase, after which elements may not be added.  The built phase begins
     * when the {@link #build()} method is called, which creates an ordered
     * {@code Stream} whose elements are the elements that were added to the stream
     * builder, in the order they were added.
     *
     * @param <T> the type of stream elements
     * @see Stream#builder()
     * @since 1.8
     */
    public interface Builder<T> extends Consumer<T> {

        /**
         * Adds an element to the stream being built.
         *
         * @throws IllegalStateException if the builder has already transitioned to
         * the built state
         */
        @Override
        void accept(T t);

        /**
         * Adds an element to the stream being built.
         *
         * @implSpec
         * The default implementation behaves as if:
         * <pre>{@code
         *     accept(t)
         *     return this;
         * }</pre>
         *
         * @param t the element to add
         * @return {@code this} builder
         * @throws IllegalStateException if the builder has already transitioned to
         * the built state
         */
        default Builder<T> add(T t) {
            accept(t);
            return this;
        }

        /**
         * Builds the stream, transitioning this builder to the built state.
         * An {@code IllegalStateException} is thrown if there are further attempts
         * to operate on the builder after it has entered the built state.
         *
         * @return the built stream
         * @throws IllegalStateException if the builder has already transitioned to
         * the built state
         */
        Stream<T> build();

    }
}
