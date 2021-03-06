package com.zhaijiong.crawler;

import com.google.common.base.Preconditions;
import com.zhaijiong.crawler.dao.ReportTemplate;
import com.zhaijiong.crawler.pipeline.MongoDBPipeline;
import com.zhaijiong.crawler.processor.BaseReportProcessor;
import com.zhaijiong.crawler.scheduler.RedisDuplicateRemover;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.scheduler.QueueScheduler;

/**
 * Created by xuqi.xq on 2014/8/12.
 */

@Configuration
@EnableAutoConfiguration
@ComponentScan
public class Main implements CommandLineRunner{

    @Autowired
    private MongoDBPipeline pipeline;

    public static void main(String[] args){
        SpringApplication.run(Main.class,args);
    }

    @Override
    public void run(String... args) throws Exception {
        Site site = Site.me().setRetryTimes(3).setSleepTime(1000);
        ReportTemplate template = ReportTemplate.build(args[0]);
        Preconditions.checkNotNull(template,"build repost template fail.please check path and file.");

        BaseReportProcessor processor = new BaseReportProcessor(template,site);

        QueueScheduler scheduler = new QueueScheduler();

        JedisPool pool = new JedisPool("zhaijiong.com",6379);
        scheduler.setDuplicateRemover(new RedisDuplicateRemover(template,pool,1000000));

        Spider.create(processor)
                .addUrl(template.getSeedUrl())
                .thread(5)
                .addPipeline(pipeline)
                .setScheduler(scheduler)
                .run();
    }
}
