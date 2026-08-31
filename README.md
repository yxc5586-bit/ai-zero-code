# AI ZeroCode 零代码应用生成平台

AI ZeroCode 是一个基于 AI 的零代码网站生成平台。用户只需要用自然语言描述想法，系统就可以创建应用、进入对话式生成工作台，持续生成和优化网站代码，并支持在线预览、代码下载、应用部署和应用管理。

## 项目展示

### 登录页

![登录页](docs/images/login.png)

### 应用首页

![应用首页](docs/images/home-dashboard.png)

### AI 生成工作台

![AI 生成工作台](docs/images/workspace.png)

## 核心功能

- 自然语言创建应用：输入一句网站需求，创建专属应用项目。
- AI 代码生成：支持原生 HTML、多文件项目、Vue 工程等生成模式。
- 流式对话工作台：AI 生成内容实时输出，右侧同步预览生成效果。
- 可视化编辑：在预览区域选择页面元素，并通过对话继续修改。
- 应用管理：支持我的应用、精选应用、应用详情、应用编辑和删除。
- 代码下载与部署：生成后可下载项目源码，也可以部署为可访问页面。
- 用户体系：支持注册、登录、登录态持久化和管理员权限校验。
- 对话历史：基于应用维度保存用户与 AI 的生成记录。
- 可观测能力：集成 Spring Boot Actuator、Prometheus 指标采集能力。

## 技术栈

### 后端

- Java 21
- Spring Boot 3.5.x
- MyBatis-Flex
- MySQL
- Redis / Redisson / Spring Session
- LangChain4j
- LangGraph4j
- Reactor SSE
- Selenium / WebDriverManager
- 腾讯云 COS
- Knife4j / SpringDoc OpenAPI
- Spring Boot Actuator / Micrometer / Prometheus

### 前端

- Vue 3
- TypeScript
- Vite
- Vue Router
- Pinia
- Ant Design Vue
- Axios
- Markdown-it
- Highlight.js

## 项目结构

```text
ai-zero-code
├── ai-zero-code-fontend/ai-zero-code-fontend  # 前端项目
├── sql                                        # 数据库初始化脚本
├── src/main/java/com/cyx/aizerocode           # 后端业务代码
├── src/main/resources                         # 后端配置、Mapper、Prompt
├── docs/images                                # README 展示截图
├── pom.xml                                    # Maven 配置
└── README.md
```

## 环境要求

- JDK 21+
- Maven 3.9+，也可以直接使用项目内置的 `mvnw`
- Node.js 22.18+ 或 24.12+
- MySQL 8.x
- Redis 5.x+

## 后端启动

1. 初始化数据库：

```bash
mysql -u root -p < sql/create_table.sql
```

2. 准备本地配置文件：

项目默认会启用 `local` profile。请在 `src/main/resources/` 下创建本地配置文件：

```text
src/main/resources/application-local.yml
```

本地配置文件不要提交到 Git。可以按下面结构填写自己的本地数据库、Redis、AI 模型和对象存储配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zero_code?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: your_mysql_password
  data:
    redis:
      host: localhost
      port: 6379
      password:
      database: 0
  session:
    redis:
      namespace: zerocode:session

langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      api-key: your_api_key
      model-name: deepseek-v4-flash
    streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: your_api_key
      model-name: deepseek-v4-flash
    reasoning-streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: your_api_key
      model-name: deepseek-v4-pro
    routing-chat-model:
      base-url: https://api.deepseek.com
      api-key: your_api_key
      model-name: deepseek-v4-flash

cos:
  client:
    host: your_cos_domain
    secretId: your_secret_id
    secretKey: your_secret_key
    region: ap-shanghai
    bucket: your_bucket

pexels:
  api-key: your_pexels_api_key

dashscope:
  api-key: your_dashscope_api_key
  image-model: wan2.2-t2i-flash

zerocode:
  code:
    output-root: /opt/resume-demo/zero-code/tmp/code_output
    deploy-root: /opt/resume-demo/zero-code/tmp/code_deploy
    public-base-url: http://PUBLIC_IP:8080
  screenshot:
    temp-root: /opt/resume-demo/zero-code/tmp/screenshots
    chrome-binary-path: /usr/bin/google-chrome
    chrome-driver-path: /usr/local/bin/chromedriver
  build:
    npm-executable: /usr/bin/npm
    npm-cache-root: /opt/resume-demo/zero-code/npm-cache
  generation:
    global-concurrency: 1
```

3. 启动 Redis。

4. 启动后端：

```bash
./mvnw spring-boot:run
```

Windows 环境可以使用：

```bash
mvnw.cmd spring-boot:run
```

后端默认访问地址：

```text
http://localhost:8123/api
```

健康检查：

```text
http://localhost:8123/api/actuator/health
```

接口文档：

```text
http://localhost:8123/api/doc.html
```

## 前端启动

进入前端目录：

```bash
cd ai-zero-code-fontend/ai-zero-code-fontend
```

安装依赖：

```bash
npm install
```

准备前端环境变量：

```bash
cp .env.example .env
```

默认配置：

```env
VITE_API_BASE_URL=/api
VITE_DEPLOY_DOMAIN=http://localhost
```

公网 IP 阶段部署 ZeroCode 前端时可设置：

```env
VITE_API_BASE_URL=/api
VITE_DEPLOY_DOMAIN=http://PUBLIC_IP:8080/deploy
```

启动前端：

```bash
npm run dev
```

前端默认访问地址：

```text
http://localhost:5173
```

## 打包构建

后端构建：

```bash
./mvnw clean package
```

前端构建：

```bash
cd ai-zero-code-fontend/ai-zero-code-fontend
npm run build
```

## 安全说明

本项目已配置 `.gitignore`，默认忽略以下敏感或临时内容：

- `application-local.yml`
- `.env`、`.env.*`
- `tmp/`
- `target/`
- `node_modules/`
- `dist/`
- 日志文件和本地 IDE 配置

请不要把真实的数据库密码、AI API Key、对象存储密钥、云服务 Token 提交到 GitHub。公开仓库中只保留占位配置或示例配置，真实配置请放在本地忽略文件、环境变量或部署平台的 Secret 配置中。

## 备注

README 中的截图来自本地运行效果，仅用于展示页面功能。不同本地数据、AI 生成结果和部署配置下，页面内容可能会有所不同。
