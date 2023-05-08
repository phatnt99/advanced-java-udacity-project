package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public class CrawlerRecursiveTask extends RecursiveTask<Boolean> {
    protected String startUrl;
    protected Instant timeToStop;
    protected Integer maxDepth;
    protected ConcurrentMap<String, Integer> counts;
    protected ConcurrentSkipListSet<String> visitedUrls;
    protected Clock clock;
    protected List<Pattern> ignoredUrls;
    protected PageParserFactory parserFactory;

    public CrawlerRecursiveTask(String startUrl, Instant timeToStop, Integer maxDepth, ConcurrentMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls, Clock clock, PageParserFactory parserFactory, List<Pattern> ignoredUrls) {
        this.startUrl = startUrl;
        this.timeToStop = timeToStop;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.clock = clock;
        this.parserFactory = parserFactory;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    protected Boolean compute() {
        if (clock.instant().isAfter(timeToStop) || maxDepth == 0) {
            System.out.println("Not meet the conditions to compute");
            return false;
        }

        if (visitedUrls.contains(startUrl)) {
            return false;
        }

        if (ignoredUrls != null) {
            for (Pattern ignoredUrl : ignoredUrls) {
                if (ignoredUrl.matcher(startUrl).matches()) {
                    return false;
                }
            }
        }

        visitedUrls.add(startUrl);
        PageParser.Result result = parserFactory.get(startUrl).parse();

        for (ConcurrentMap.Entry<String, Integer> entry : result.getWordCounts().entrySet()) {
            counts.compute(entry.getKey(), (k, v) -> v == null ? entry.getValue() : entry.getValue() + v);
        }

        List<CrawlerRecursiveTask> subTasks = new ArrayList<>();

        for (String link : result.getLinks()) {
            subTasks.add(new CrawlerRecursiveTask(link, timeToStop, maxDepth - 1, counts, visitedUrls, clock, parserFactory, ignoredUrls));
        }

        invokeAll(subTasks);

        return true;
    }


}
