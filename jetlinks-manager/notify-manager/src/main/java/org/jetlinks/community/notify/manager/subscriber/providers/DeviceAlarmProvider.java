package org.jetlinks.community.notify.manager.subscriber.providers;

import com.alibaba.fastjson.JSONObject;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DefaultConfigMetadata;
import org.jetlinks.core.metadata.types.StringType;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.gateway.MessageGateway;
import org.jetlinks.community.gateway.Subscription;
import org.jetlinks.community.notify.manager.subscriber.Notify;
import org.jetlinks.community.notify.manager.subscriber.Subscriber;
import org.jetlinks.community.notify.manager.subscriber.SubscriberProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class DeviceAlarmProvider implements SubscriberProvider {

    private final MessageGateway messageGateway;

    public DeviceAlarmProvider(MessageGateway messageGateway) {
        this.messageGateway = messageGateway;
    }

    @Override
    public String getId() {
        return "device_alarm";
    }

    @Override
    public String getName() {
        return "设备告警";
    }

    @Override
    public ConfigMetadata getConfigMetadata() {
        return new DefaultConfigMetadata()
            .add("productId", "产品ID", "产品ID,支持通配符:*", StringType.GLOBAL)
            .add("deviceId", "设备ID", "设备ID,支持通配符:*", StringType.GLOBAL)
            .add("productId", "告警ID", "告警ID,支持通配符:*", StringType.GLOBAL)
            ;
    }

    @Override
    public Mono<Subscriber> createSubscriber(Map<String, Object> config) {
        ValueObject configs = ValueObject.of(config);

        String productId = configs.getString("productId").orElse("*");
        String deviceId = configs.getString("deviceId").orElse("*");
        String alarmId = configs.getString("alarmId").orElse("*");

        Flux<Notify> flux = messageGateway
            .subscribe(Subscription.asList(
                String.format("/rule-engine/device/alarm/%s/%s/%s", productId, deviceId, alarmId)),
                messageGateway.nextSubscriberId("device-alarm-notifications"),
                false)
            .map(msg -> {
                JSONObject json = msg.getMessage().payloadAsJson();

                return Notify.of(
                    String.format("设备[%s]发生告警:[%s]!", json.getString("deviceName"), json.getString("alarmName")),
                    json.getString("alarmId"),
                    System.currentTimeMillis()
                );

            });

        return Mono.just(() -> flux);
    }
}
