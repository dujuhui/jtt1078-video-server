package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.publisher.entity.Media;
import cn.org.hentai.jtt1078.publisher.entity.Video;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by houcheng on 2019-12-11.
 */
public final class PublishManager
{
    static Logger logger = LoggerFactory.getLogger(PublishManager.class);

    ConcurrentHashMap<String, ConcurrentLinkedDeque<Media>> channelMap;
    ConcurrentHashMap<String, ConcurrentLinkedQueue<Subscriber>> subscriberMap;

    private PublishManager()
    {
        channelMap = new ConcurrentHashMap<String, ConcurrentLinkedDeque<Media>>();
        subscriberMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Subscriber>>();
    }

    // 1. 订阅是怎么订阅的？

    // 2. 需要持续持有关键祯

    // 3. 需要能够删除上一个系列

    // 4. 需要记录每个订阅者的索引

    // 5. 需要提供视频与音频
    public void subscribe(String tag, ChannelHandlerContext ctx)
    {
        ConcurrentLinkedQueue<Subscriber> listeners = subscriberMap.get(tag);
        if (listeners == null)
        {
            listeners = new ConcurrentLinkedQueue<Subscriber>();
            subscriberMap.put(tag, listeners);
        }

        Subscriber subscriber = new Subscriber(tag, ctx);
        subscriber.setName("subscriber-" + ctx.channel().remoteAddress().toString());
        listeners.add(subscriber);
        subscriber.start();

        // 如果已经有视频流了，那就把前三个关键片断发过去
        ConcurrentLinkedDeque<Media> segments = channelMap.get(tag);
        if (segments != null)
        {
            int i = 0;
            for (Media media : segments)
            {
                if (i++ == 3) break;
                subscriber.aware(media);
            }
        }
    }

    public void publish(String tag, Media media)
    {
        ConcurrentLinkedDeque<Media> segments = channelMap.get(tag);
        if (segments == null)
        {
            segments = new ConcurrentLinkedDeque<Media>();
            channelMap.put(tag, segments);
        }

        // 只用缓存前三个消息包就可以了
        if (segments.size() < 3) segments.addLast(media);

        // 广播到所有的订阅者，直接发，先不等待关键祯
        ConcurrentLinkedQueue<Subscriber> listeners = subscriberMap.get(tag);
        if (listeners == null)
        {
            listeners = new ConcurrentLinkedQueue<Subscriber>();
            subscriberMap.put(tag, listeners);
        }
        for (Subscriber listener : listeners)
        {
            try
            {
                listener.aware(media);
            }
            catch(Exception ex)
            {
                logger.error("send error", ex);
                listeners.remove(listener);
            }
        }
    }

    static final PublishManager instance = new PublishManager();
    public static void init() { }
    public static PublishManager getInstance()
    {
        return instance;
    }
}