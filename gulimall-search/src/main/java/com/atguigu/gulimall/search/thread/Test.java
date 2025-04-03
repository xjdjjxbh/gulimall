package com.atguigu.gulimall.search.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Test {
    //线程池
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {


        Object res = 5L;
        System.out.println(res);


        CompletableFuture<Integer> completableFuture = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1线程开始执行:" + Thread.currentThread().getName());
            int i = 10 / 2;
            System.out.println("任务1运行结果:" + i);
            return i;
        }, executorService);

        CompletableFuture<String> completableFuture2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务2线程开始:" + Thread.currentThread().getName());
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("任务2线程结束:" + Thread.currentThread().getName());
            return "hello";
        }, executorService);

//        completableFuture.runAfterBothAsync(completableFuture2, () -> {
//            System.out.println("前两个任务执行完成,任务3开始执行");
//        }, executorService);

//        completableFuture.thenAcceptBothAsync(completableFuture2, (r1, r2) -> {
//            System.out.println("任务1的执行结果是:" + r1);
//            System.out.println("任务1的执行结果是:" + r2);
//        }, executorService);
        CompletableFuture<String> completableFuture3 = completableFuture.thenCombineAsync(completableFuture2, (t, u) -> {
            System.out.println("任务一执行结果:" + t);
            System.out.println("任务二执行结果:" + u);
            return t + "->" + u + "->" + "haha";
        }, executorService);
        completableFuture3.thenAccept(System.out::println);
    }
}
