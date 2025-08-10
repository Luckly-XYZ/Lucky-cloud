package com.xy.spring.utils;

import com.xy.spring.context.ApplicationContext;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * YAML 配置加载器
 * <p>
 * 负责：
 * 1. 从 classpath 读取 application.yml，
 * 2. 将嵌套结构扁平化为 <code>key=value</code> 形式的 Map,
 * 3. 支持基础类型、数组、List、Map 等类型转换，
 * 4. 提供手动覆写功能。
 * </p>
 */
public class YamlConfigLoader {

    /**
     * 扁平化后配置项存储：
     * key: 配置路径（用 '.' 分隔），value: 原始值对象
     */
    private static final Map<String, Object> properties = new LinkedHashMap<>();

    /**
     * 私有构造，禁止实例化
     */
    private YamlConfigLoader() {}

    /**
     * 加载并解析 application.yml
     * <p>建议在应用启动时调用一次，例如在 {@link ApplicationContext} 中执行。</p>
     *
     * @throws RuntimeException 如果文件不存在或解析失败
     */
    public static void load() {
        try (InputStream in = getResourceStream("application.yml")) {
            Map<String, Object> yamlData = new Yaml().load(in);
            if (yamlData != null) {
                flatten("", yamlData);
            }
        } catch (Exception e) {
            throw new RuntimeException("YAML 读取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 classpath 获取资源流
     */
    private static InputStream getResourceStream(String resourcePath) {
        InputStream in = YamlConfigLoader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new RuntimeException("未找到资源文件: " + resourcePath);
        }
        return in;
    }

    /**
     * 扁平化嵌套 Map
     *
     * @param prefix 当前键前缀，首次调用请传空字符串
     * @param map    当前层级的键值对
     */
    @SuppressWarnings("unchecked")
    private static void flatten(String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                flatten(key, (Map<String, Object>) value);
            } else {
                properties.put(key, value);
            }
        }
    }

    /**
     * 获取配置值并转换为指定类型
     *
     * @param key  配置项路径，例如 "redis.host"
     * @param type 目标返回类型，例如 Integer.class、List.class
     * @param <T>  类型泛型
     * @return 转换后的值，若不存在或转换失败返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key, Class<T> type) {
        Object raw = properties.get(key);
        if (raw == null) {
            return null;
        }
        // 直接类型匹配
        if (type.isInstance(raw)) {
            return (T) raw;
        }
        // 转换字符串
        String val = raw.toString().trim();
        return (T) convertValue(type, val, null);
    }

    /**
     * 获取配置值并转换为字段类型，支持泛型推断
     *
     * @param key   配置项路径
     * @param field 目标字段，用于获取泛型信息
     * @return 转换后的值，若不存在或转换错误抛异常
     */
    public static Object get(String key, Field field) {
        Object raw = properties.get(key);
        if (raw == null) {
            return null;
        }
        String val = raw.toString().trim();
        return convertValue(field.getType(), val, field);
    }

    /**
     * 手动添加或覆盖配置项
     *
     * @param key   配置路径
     * @param value 值（字符串形式）
     */
    public static void put(String key, String value) {
        properties.put(key, value);
    }

    /**
     * 获取全部配置快照，调试用
     */
    public static Map<String, Object> all() {
        return Collections.unmodifiableMap(properties);
    }

    /**
     * 核心转换逻辑
     */
    private static Object convertValue(Class<?> type, String val, Field field) {
        if (Objects.equals(type, String.class)) {
            return val;
        } else if (type == int.class || type == Integer.class) {
            return Integer.parseInt(val);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(val);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(val);
        } else if (type.isArray()) {
            return convertArray(type.getComponentType(), val);
        } else if (List.class.isAssignableFrom(type) && field != null) {
            return convertToList(val, field);
        } else if (Map.class.isAssignableFrom(type) && field != null) {
            return convertToMap(val, field);
        }
        // 未知类型，返回原字符串
        return val;
    }

    /**
     * 转换为数组
     */
    private static Object convertArray(Class<?> componentType, String val) {
        String[] parts = splitTrim(val);
        Object array = Array.newInstance(componentType, parts.length);
        for (int i = 0; i < parts.length; i++) {
            Array.set(array, i, convertSimpleValue(componentType, parts[i]));
        }
        return array;
    }

    /**
     * 转换为 List<T>
     */
    @SuppressWarnings("unchecked")
    private static List<Object> convertToList(String val, Field field) {
        String[] parts = splitTrim(val);
        List<Object> list = new ArrayList<>();
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType) {
            Type itemType = ((ParameterizedType) generic).getActualTypeArguments()[0];
            for (String part : parts) {
                list.add(convertSimpleValue((Class<?>) itemType, part));
            }
        }
        return list;
    }

    /**
     * 转换为 Map<String, V>
     */
    private static Map<String, Object> convertToMap(String val, Field field) {
        Map<String, Object> map = new LinkedHashMap<>();
        String content = val.replaceAll("^\\{|\\}$", "");
        for (String entry : content.split(",")) {
            String[] kv = entry.split("=");
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    /**
     * 将简单值转为基础类型
     */
    private static Object convertSimpleValue(Class<?> type, String val) {
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(val);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(val);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(val);
        }
        return val;
    }

    /**
     * 按逗号分隔并去除空白
     */
    private static String[] splitTrim(String val) {
        return Arrays.stream(val.replaceAll("[\\[\\]\\s]", "").split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }
}