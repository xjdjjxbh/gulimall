package com.atguigu.gulimall.order;

import com.atguigu.gulimall.order.entity.OrderReturnReasonEntity;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@Slf4j
@SpringBootTest
class GulimallOrderApplicationTests {


    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Test
    void contextExchange() {
        amqpAdmin.declareExchange(new TopicExchange("order-event-exchange", true, false));
        log.info("交换机创建成功");
    }

    @Test
    void contextQueue() {
        amqpAdmin.declareQueue(new Queue("order-event-queue", true));
        log.info("队列创建成功");
    }

    @Test
    void contextBinding() {
        amqpAdmin.declareBinding(new Binding("order-event-queue",
                Binding.DestinationType.QUEUE, "order-event-exchange",
                "order.event", null));
        log.info("绑定创建成功");
    }

    @Test
    void sendMsg() {
        //如果发送的消息是一个对象，那么这个对象必须实现序列化，不然无法经过网络发送
        OrderReturnReasonEntity orderReturnReasonEntity = new OrderReturnReasonEntity();
        orderReturnReasonEntity.setId(1L);
        orderReturnReasonEntity.setCreateTime(new Date());
        orderReturnReasonEntity.setName("test");
        orderReturnReasonEntity.setStatus(1);
        amqpTemplate.convertAndSend("order-event-exchange", "order.event", orderReturnReasonEntity);
        log.info("消息发送成功");
    }

}
