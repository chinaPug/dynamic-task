package cn.pug.dynamic.task.core.util;


import java.util.Comparator;

/**
 * 版本号比较器
 * 支持格式如: 1, 1.0, 1.0.0, 2.1.3, 10.15.2 等一位、两位或三位版本号比较
 */
public class VersionComparator implements Comparator<String> {
    
    @Override
    public int compare(String version1, String version2) {
        if (version1 == null || version2 == null) {
            throw new IllegalArgumentException("版本号不能为空");
        }
        
        // 将版本号字符串分割为数字部分
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        // 验证版本号格式(支持1位、2位或3位版本号)
        if (v1Parts.length < 1 || v1Parts.length > 3 || 
            v2Parts.length < 1 || v2Parts.length > 3) {
            throw new IllegalArgumentException("版本号必须是一位、两位或三位格式，如 1, 1.0, 1.0.0");
        }
        
        // 获取最大比较位数
        int maxLen = Math.max(v1Parts.length, v2Parts.length);
        
        // 逐位比较版本号
        for (int i = 0; i < maxLen; i++) {
            try {
                // 如果某版本号位数不够，默认该位为0
                int num1 = (i < v1Parts.length) ? Integer.parseInt(v1Parts[i]) : 0;
                int num2 = (i < v2Parts.length) ? Integer.parseInt(v2Parts[i]) : 0;
                
                // 如果当前位不相等，直接返回比较结果
                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("版本号必须由数字组成", e);
            }
        }
        
        // 所有位都相等，返回0
        return 0;
    }
    
    /**
     * 验证版本号格式是否正确
     * @param version 版本号字符串
     * @return 是否为合法的一位、两位或三位版本号
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.isEmpty()) {
            return false;
        }
        
        String[] parts = version.split("\\.");
        // 支持1位、2位或3位版本号
        if (parts.length < 1 || parts.length > 3) {
            return false;
        }
        
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return true;
    }

}
