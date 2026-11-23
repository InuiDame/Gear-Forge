# GF-019 没有 CI，版本号和发布流程不统一

- 优先级：P2
- 状态：待决议（Q14）
- 相关：`docs/development.md` 第 5 节

## 问题

- tag 有 `1.6.0`、`1.6.1`、`1.6.2`、`1.6.4`、`1.64fix`、`1.65`、`1.651`，格式各异；`gradle.properties` 里是 `1.6.51`。从 tag 看不出 1.651 和 1.6.51 是不是同一个版本。
- 提交信息多为 `update` / `updata` / `First`，无法从历史知道每个版本改了什么。
- 没有 CI。整理前 `gradle-wrapper.jar` 没提交，全新克隆根本跑不起 `gradlew`，没有人发现。
- 发布渠道和发布步骤没有记录。

## 修复方案

1. 版本号：语义化版本 `主.次.修订`，tag 用 `v<mod_version>`（例如 `v1.6.51`）。已有 tag 保留，不改写历史。
2. 维护 `CHANGELOG.md`，每次发布写清改动；平衡类变化单独列出。
3. GitHub Actions：
   - `build.yml`：push 和 PR 时 `./gradlew build`（GF-018 完成后加 `test runGameTestServer`）。
   - `release.yml`：推送 `v*` tag 时构建，创建 GitHub Release 并上传 jar；校验 tag 与 `mod_version` 一致。
   - wrapper 的阿里云镜像在 GitHub 托管机上可能较慢，CI 里可以通过环境变量覆盖为官方地址。
4. Modrinth / CurseForge 上传视决议，可以用 `mc-publish` 这类 Action 自动化，需要在仓库 Secrets 里配置 token。

## 验收标准

推送 `v1.6.52` tag 后，GitHub Release 自动出现对应 jar；tag 与 `mod_version` 不一致时发布失败。
