package com.mall.web.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

public final class RedisClient {
    private static final Properties PROPS = new Properties();
    private static final JedisPool POOL;

    static {
        try (InputStream inputStream = RedisClient.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (inputStream != null) {
                PROPS.load(inputStream);
            }
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxTotal(16);
            config.setMaxIdle(8);
            config.setMinIdle(1);
            config.setTestOnBorrow(true);
            String host = config("REDIS_HOST", "redis.host", "localhost");
            int port = Integer.parseInt(config("REDIS_PORT", "redis.port", "6379"));
            String password = config("REDIS_PASSWORD", "redis.password", "");
            POOL = password.isBlank()
                    ? new JedisPool(config, host, port, 3000)
                    : new JedisPool(config, host, port, 3000, password);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private RedisClient() {
    }

    public static Optional<String> get(String key) {
        try (Jedis jedis = POOL.getResource()) {
            return Optional.ofNullable(jedis.get(key));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public static void setex(String key, Duration ttl, String value) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.setex(key, (int) ttl.toSeconds(), value);
        } catch (Exception ignored) {
        }
    }

    public static void del(String key) {
        try (Jedis jedis = POOL.getResource()) {
            jedis.del(key);
        } catch (Exception ignored) {
        }
    }

    public static void delByPrefix(String prefix) {
        try (Jedis jedis = POOL.getResource()) {
            for (String key : jedis.keys(prefix + "*")) {
                jedis.del(key);
            }
        } catch (Exception ignored) {
        }
    }

    private static String config(String envName, String propName, String defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            value = System.getProperty(envName);
        }
        if (value == null || value.isBlank()) {
            value = PROPS.getProperty(propName, defaultValue);
        }
        return value == null ? defaultValue : value;
    }
}
