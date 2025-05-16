# Dynamic Task Framework

Dynamic Task Framework 是一个基于 Spring Boot Starter 的动态插件热部署框架，支持插件级别的动态加载、热更新和自动化发布，专为资源受限场景（如RPA客户端）设计，极大提升了系统的灵活性和可维护性。

## 特性

- 🚀 动态加载：支持本地和远程按需加载插件（JAR包）
- 🔄 热部署与热更新：插件可在运行时动态加载、升级、卸载，无需重启主程序
- 🛡️ 版本管理：内置插件版本控制和兼容性校验
- 💡 内存优化：采用软引用和引用队列，自动回收不活跃插件，适配低内存环境
- 📦 模块化：多模块架构，便于扩展和维护
- 🔌 零侵入集成：Spring Boot Starter自动装配，开箱即用
- ⚙️ 自动化发布：支持插件级别的增量发布和远程分发
- 📚 开发规范：统一的插件开发、打包、版本管理规范

## 项目结构

项目采用多模块设计，包含以下模块：

- `dynamic-task-api`: 核心接口和模型定义，约束插件开发规范
- `dynamic-task-core`: 核心功能实现，包括任务调度、插件生命周期管理、插件加载与卸载、内存优化等机制
- `dynamic-task-starter`: Spring Boot 启动器与自动配置
- `dynamic-task-example`: 使用示例与最佳实践

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>cn.pug</groupId>
    <artifactId>dynamic-task-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置

在 `application.yml` 中添加配置：

```yaml
script:
  enable: true
  local-jar-path: /path/to/local/jars
  remote-jar-url: ftp://your-ftp-server/jars
  enabled-banner: true
```

### 3. 使用

```java
@Autowired
private Actuator actuator;

public void executeTask() {
    Event event = Event.builder()
        .taskId("task-1")
        .identifyVal("script-name")
        .scriptVersion("1.0.0")
        .build();
    
    CompletableFuture<Result> future = actuator.submit(event);
    // 处理结果...
}
```

## 插件开发与部署

1. **开发插件脚本**

```java
@Script(identifyVal = "my-task", version = "1.0.0")
public class MyTask implements Scene<String> {
    @Override
    public CompletableFuture<Result> action(Event event) {
        // 实现任务逻辑
        return CompletableFuture.completedFuture(Result.success(event.getTaskId()));
    }
}
```

2. **打包为 JAR 文件**
   - 命名格式：`{identifyVal}-{version}.jar` 例如：`my-task-1.0.0.jar`

3. **部署插件**
   - 将 JAR 文件放到配置的本地路径，或上传到配置的远程服务器

## 关键设计与实现

### 1. 动态热部署与内存优化
- 使用自定义类加载器（DynamicScriptClassLoader）实现插件的隔离加载与卸载
- 通过软引用（SoftReference）和引用队列（ReferenceQueue）管理插件实例，内存不足时自动回收
- 定期清理无效引用，保证内存可控

### 2. 插件生命周期与版本管理
- 插件注册、加载、卸载均由DynamicScriptAcquirable统一管理
- 通过@Script注解标记插件元数据，支持多版本共存与升级
- 版本兼容性校验，防止降级和冲突

### 3. 自动化发布与集成
- 支持插件级别的独立打包与增量发布
- 支持本地和远程JAR包的自动拉取与热更新
- Spring Boot Starter自动装配，零侵入集成

### 4. 插件开发规范
- 统一接口（Scene、Event、Result等）和注解（@Script）约束
- 规范异常处理、日志记录和版本号管理

## 注意事项

1. 插件版本号必须递增，遵循语义化版本规范
2. 插件类需实现Scene接口并加@Script注解
3. 插件JAR包命名需规范，便于自动识别和加载
4. 低内存环境建议合理设置本地插件缓存路径

## 贡献

欢迎提交 Issue 和 Pull Request。

## 许可证

[MIT License](LICENSE)