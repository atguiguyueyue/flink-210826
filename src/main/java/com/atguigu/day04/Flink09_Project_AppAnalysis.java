package com.atguigu.day04;

import com.atguigu.bean.MarketingUserBehavior;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Flink09_Project_AppAnalysis {
    public static void main(String[] args) throws Exception {
        //1.获取流的执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        //2.从自定义数据源获取数据
        DataStreamSource<MarketingUserBehavior> streamSource = env.addSource(new AppMarketingDataSource());

        //3.将数据转为Tuple2元组，key：渠道+行为 value：1
        SingleOutputStreamOperator<Tuple2<String, Integer>> channelWithBehaviorToOneDStream = streamSource.map(new MapFunction<MarketingUserBehavior, Tuple2<String, Integer>>() {
            @Override
            public Tuple2<String, Integer> map(MarketingUserBehavior value) throws Exception {
                return Tuple2.of(value.getBehavior(), 1);
            }
        });

        //4.对于相同渠道相同行为的数据聚和到一块
        KeyedStream<Tuple2<String, Integer>, Tuple> keyedStream = channelWithBehaviorToOneDStream.keyBy(0);

        //5.统计个数
        SingleOutputStreamOperator<Tuple2<String, Integer>> result = keyedStream.sum(1);

        result.print();
        env.execute();
    }

    public static class AppMarketingDataSource extends RichSourceFunction<MarketingUserBehavior> {
        boolean canRun = true;
        Random random = new Random();
        List<String> channels = Arrays.asList("huawwei", "xiaomi", "apple", "baidu", "qq", "oppo", "vivo");
        List<String> behaviors = Arrays.asList("download", "install", "update", "uninstall");

        @Override
        public void run(SourceContext<MarketingUserBehavior> ctx) throws Exception {
            while (canRun) {
                MarketingUserBehavior marketingUserBehavior = new MarketingUserBehavior(
                        (long) random.nextInt(1000000),
                        behaviors.get(random.nextInt(behaviors.size())),
                        channels.get(random.nextInt(channels.size())),
                        System.currentTimeMillis());
                ctx.collect(marketingUserBehavior);
                Thread.sleep(200);
            }
        }

        @Override
        public void cancel() {
            canRun = false;
        }
    }

}
