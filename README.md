# Dynamic Task Framework

Dynamic Task Framework 是一个基于 Spring Boot Starter 的动态插件热部署框架，支持插件级别的动态加载、热更新和自动化发布，专为资源受限场景（如RPA客户端）设计，极大提升了系统的灵活性和可维护性。

## 🌟 特性

- 🚀 **动态加载**：支持本地和远程按需加载插件（JAR包）
- 🔄 **热部署与热更新**：插件可在运行时动态加载、升级、卸载，无需重启主程序
- 🛡️ **版本管理**：内置插件版本控制和兼容性校验
- 💡 **内存优化**：采用软引用和引用队列，自动回收不活跃插件，适配低内存环境
- 📦 **模块化**：多模块架构，便于扩展和维护
- 🔌 **零侵入集成**：Spring Boot Starter自动装配，开箱即用
- ⚙️ **本地部署**：支持插件级别的本地打包和部署
- 📚 **开发规范**：统一的插件开发、打包、版本管理规范
- 🔧 **灵活配置**：丰富的配置选项，适应不同部署环境
- 📊 **监控支持**：内置性能监控和日志记录

## 🏗️ 项目结构

项目采用多模块设计，包含以下模块：

```
dynamic-task/
├── dynamic-task-api/           # 核心接口和模型定义
├── dynamic-task-common/        # 公共工具与基础组件
├── dynamic-task-core/          # 核心功能实现
├── dynamic-task-starter/       # Spring Boot 启动器
└── dynamic-task-example/       # 使用示例与最佳实践
```

- **`dynamic-task-api`**: 核心接口和模型定义，约束插件开发规范
- **`dynamic-task-common`**: 公共工具与基础组件，提供常用工具类、常量、Spring环境支持等
- **`dynamic-task-core`**: 核心功能实现，包括任务调度、插件生命周期管理、插件加载与卸载、内存优化等机制
- **`dynamic-task-starter`**: Spring Boot 启动器与自动配置
- **`dynamic-task-example`**: 使用示例与最佳实践

## 🚀 快速开始

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
  # 启用动态任务框架
  enable: true
  # 本地插件JAR文件路径
  local-jar-path: /path/to/local/jars
  # 启用启动横幅
  enabled-banner: true
  # 执行器配置（可选）
  executor:
    - name: "default-executor"
      core-pool-size: 5
      max-pool-size: 10
      queue-capacity: 1000
      keep-alive-seconds: 60
      task-rejected-policy: "DEFAULT"
```

### 3. 使用

```java
@Autowired
private Actuator actuator;

public void executeTask() {
    Event inputWrapper = Event.builder()
        .taskId("task-1")
        .identifyVal("script-name")
        .scriptVersion("1.0.0")
        .build();
    
    CompletableFuture<Result> future = actuator.submit(inputWrapper);
    
    // 异步处理结果
    future.thenAccept(result -> {
        if (result.isSuccess()) {
            System.out.println("任务执行成功: " + result.getData());
        } else {
            System.err.println("任务执行失败: " + result.getErrorMessage());
        }
    });
}
```

## 🔧 详细配置说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `script.enable` | Boolean | false | 是否启用动态任务框架 |
| `script.local-jar-path` | String | null | 本地插件JAR文件存储路径 |
| `script.enabled-banner` | Boolean | true | 是否显示启动横幅 |
| `script.executor` | List | [] | 执行器配置列表 |
| `script.executor[].name` | String | null | 执行器名称标识 |
| `script.executor[].core-pool-size` | Integer | null | 核心线程池大小 |
| `script.executor[].max-pool-size` | Integer | null | 最大线程池大小 |
| `script.executor[].queue-capacity` | Integer | 1000 | 任务队列容量 |
| `script.executor[].keep-alive-seconds` | Integer | 60 | 空闲线程存活时间（秒） |
| `script.executor[].task-rejected-policy` | String | "DEFAULT" | 任务拒绝策略 |

### 路径配置示例

```yaml
# 使用绝对路径
script:
  local-jar-path: /opt/dynamic-task/plugins

# 使用相对路径
script:
  local-jar-path: ./plugins

# 使用classpath资源
script:
  local-jar-path: classpath:plugins/

# 执行器配置示例
script:
  executor:
    - name: "fast-executor"       # 快速任务执行器
      core-pool-size: 2
      max-pool-size: 5
      queue-capacity: 100
      keep-alive-seconds: 30
      task-rejected-policy: "CALLER_RUNS"
    - name: "heavy-executor"      # 重任务执行器
      core-pool-size: 5
      max-pool-size: 15
      queue-capacity: 2000
      keep-alive-seconds: 120
      task-rejected-policy: "ABORT"
```

## 📝 插件开发与部署

### 1. 开发插件脚本

```java
@Script(identifyVal = "my-task", version = "1.0.0", description = "我的自定义任务")
public class MyTask implements Scene<String> {
    
    private static final Logger logger = LoggerFactory.getLogger(MyTask.class);
    
    @Override
    public CompletableFuture<Result> action(Event inputWrapper) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.debug("开始执行任务: {}", inputWrapper.getTaskId());
                
                // 实现任务逻辑
                String result = processTask(inputWrapper);
                
                logger.debug("任务执行完成: {}", inputWrapper.getTaskId());
                return Result.success(result);
                
            } catch (Exception e) {
                logger.error("任务执行失败: {}", inputWrapper.getTaskId(), e);
                return Result.failure(e.getMessage());
            }
        });
    }
    
    private String processTask(Event event) {
        // 具体业务逻辑实现
        return "处理结果";
    }
}
```

### 2. 插件项目结构

```
my-plugin/
├── pom.xml
└── src/
    └── main/
        ├── java/
        │   └── com/example/MyTask.java
        └── resources/
            └── plugin.properties  # 可选的插件配置文件
```

### 3. 打包配置（pom.xml）

```xml
<build>
    <finalName>${project.artifactId}-${project.version}</finalName>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-shade-plugin</artifactId>
            <version>3.2.4</version>
            <executions>
                <execution>
                    <phase>package</phase>
                    <goals>
                        <goal>shade</goal>
                    </goals>
                    <configuration>
                        <createDependencyReducedPom>false</createDependencyReducedPom>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### 4. 部署插件

**命名规范**：`{identifyVal}-{version}.jar`
- 例如：`my-task-1.0.0.jar`

**部署方式**：
- **本地部署**：将JAR文件放到配置的本地路径

## 🏛️ 核心架构与设计

### 系统架构

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │    │   Actuator      │    │  Plugin Manager │
│   Layer         │───▶│   (Entry Point) │───▶│   (Lifecycle)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       │
                                ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Task Queue    │    │   Class Loader  │    │  Memory Manager │
│   (Async Exec)  │    │   (Isolation)   │    │  (Soft Ref)     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### 关键设计理念

#### 1. 动态热部署与内存优化
- 使用自定义类加载器（`DynamicScriptClassLoader`）实现插件的隔离加载与卸载
- 通过软引用（`SoftReference`）和引用队列（`ReferenceQueue`）管理插件实例
- 内存不足时自动回收不活跃插件，定期清理无效引用

#### 2. 插件生命周期与版本管理
- 插件注册、加载、卸载均由`DynamicScriptAcquirable`统一管理
- 通过`@Script`注解标记插件元数据，支持多版本共存与升级
- 版本兼容性校验，防止降级和冲突

#### 3. 集成与部署
- 支持插件级别的独立打包与本地部署
- Spring Boot Starter自动装配，零侵入集成
- 支持插件的动态加载和生命周期管理

## 📋 最佳实践

### 1. 插件开发规范

- ✅ 实现`Scene`接口并添加`@Script`注解
- ✅ 遵循语义化版本规范（Semantic Versioning）
- ✅ 合理处理异常，避免插件崩溃影响主程序
- ✅ 使用适当的日志级别记录关键信息
- ✅ 保持插件无状态设计，避免内存泄漏

### 2. 性能优化建议

- 🔧 根据实际并发需求配置执行器的线程池参数
- 🔧 合理设置任务队列容量，避免内存溢出
- 🔧 选择合适的任务拒绝策略，处理高并发场景
- 🔧 监控插件执行时间，优化性能瓶颈
- 🔧 定期清理不使用的插件版本

### 3. 生产环境部署

- 🚀 确保插件JAR文件路径的安全性和可访问性
- 🚀 配置合适的执行器参数以适应生产环境负载
- 🚀 配置日志轮转，避免日志文件过大
- 🚀 定期备份重要插件版本

## 🔍 故障排除

### 常见问题

**Q: 插件加载失败，提示ClassNotFoundException**
```
A: 检查插件JAR文件是否包含所有依赖，建议使用maven-shade-plugin打包
```

**Q: 线程池资源不足**
```
A: 调整executor配置中的core-pool-size和max-pool-size参数，增加线程池容量
```

**Q: 插件执行超时**
```
A: 检查插件逻辑是否存在死循环或长时间阻塞操作
```

### 调试技巧

1. **启用详细日志**：
```yaml
logging:
  level:
    cn.pug.dynamic.task: DEBUG
```

2. **监控插件状态**：
```java
@Autowired
private PluginManager pluginManager;

// 获取已加载插件信息
List<PluginInfo> plugins = pluginManager.getLoadedPlugins();
```

## 🤝 贡献指南

欢迎参与项目贡献！请遵循以下步骤：

1. Fork 项目仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

### 开发环境

- JDK 8+
- Maven 3.6+
- Spring Boot 2.x

## 📄 许可证

本项目采用 [MIT License](LICENSE) 许可证。

## 📞 联系我们

- 项目主页：[GitHub Repository]
- 问题反馈：[Issues]
- 邮箱：developer@example.com

---

<div align="center">
  <sub>Built with ❤️ by the Dynamic Task Framework Team</sub>
</div>