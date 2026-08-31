# ZeroCode 直接部署模板

本目录保存腾讯云轻量应用服务器和 1Panel 场景下的可审查模板。业务后端以 Fat JAR 运行，前端 dist 由 OpenResty 托管，不为业务应用构建 Docker 镜像。

## 固定路径

```text
/opt/resume-demo/zero-code/app/ai-zero-code.jar
/opt/resume-demo/zero-code/web/
/opt/resume-demo/zero-code/logs/
/opt/resume-demo/zero-code/bin/
/opt/resume-demo/zero-code/tmp/code_output/
/opt/resume-demo/zero-code/tmp/code_deploy/
/opt/resume-demo/zero-code/tmp/screenshots/
/opt/resume-demo/zero-code/npm-cache/
/opt/resume-demo/config/zero-code.env
```

部署时把 `env/zero-code.env.example` 复制到服务器受限路径并填写真实值，权限建议设为 `root:zerocode 0640`。不要把真实环境文件提交到 Git。

安装前依次检查：

1. `ai-zero-code.jar` 和前端 dist 来自同一次已验证构建。
2. Java 21、Node.js 22、npm、Chrome 和 ChromeDriver 可由 `zerocode` 用户执行。
3. MySQL 和 Redis 只绑定回环地址，Redis 使用 DB 0。
4. 将 `wait-for-health.sh` 放到 `/opt/resume-demo/zero-code/bin/` 并赋予执行权限。
5. 使用 `systemd-analyze verify` 检查 service，再执行 `daemon-reload`。
6. 在 1Panel 中应用 OpenResty 配置前先执行配置语法检查。

`cleanup-zerocode-artifacts.sh` 默认只预览清理目标。第 6 天完成过期产物提示验收前，不启用定时删除；确认后使用 `--execute` 执行实际清理。
