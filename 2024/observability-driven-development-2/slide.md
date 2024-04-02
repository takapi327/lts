---
marp: true
---

# Svelte5 x Scala3 ~オブザーバビリティ駆動開発~

Nextbeat
2024/04/12 (金)

富永 孝彦

---

# 前回のあらすじ

オブザーバビリティ駆動開発 2024/03/15 (金) を見てね

---

## オブザーバビリティは

- 予期せぬトラブルを防ぐ仕組み
- 可観測性が重要
- メトリクス x ログ x トレースの組み合わせ

---

ハイパーメニーメニートゥーリッチローカル開発環境

---

# 今日は何を話す？　

- フロントエンドとバックエンドの型安全性
- フロントエンドとバックエンドで言語が異なる時のモノレポ
- フロントエンドでの可観測性
- フロントエンドとバックエンドの可観測性

---

## 改めて技術スタックは

Svelte5 x Scala3

Svelteが仲間入り！

---

### バックエンドの構成

Typelevelスタック with tapir

- バックエンド: Scala 3 x Cats Effect 3 x http4s x tapir x ldbc
- DB: MySQL v8
- 計装: Otel4s

---

### フロントの構成

- フロントエンド: Svelte 5 x SvelteKit x express x Zod x Zodios (Typescript)
- 計装: opentelemetry

---

## さらなる型安全性を求めて

APIの型安全性を高めるために、ZodとZodiosを使用

---

Scala x Tapirで定義したAPIの型をOpenAPIの仕様書として使用

OpenAPI仕様書からZodiosとZodを使用したAPIクライアントを生成し、フロントエンドにも型を伝播

---

## 何が嬉しいのか？

- バックエンドとフロントエンドの型が一致
  - コンパイルで型の不整合を検知
- バックエンドの変更に伴うフロントエンドの修正が減る
  - 修正漏れがあればコンパイルエラーで検知

---

フロントが楽になる

- フロントエンドの型安全性が向上
- フロントエンドの開発速度が向上
- フロントエンドの品質が向上
- フロントエンドのテストが楽になる
- フロントエンドのリファクタリングが楽になる
- etc...

バックエンドも楽になる

- 送られてくる内容は全て型で表現される
- テストが楽になる
- リファクタリングが楽になる
- etc...

---

## NodeとJVMのモノレポ？

フロントエンドとバックエンドで使用技術が別れていると環境構築やテスト、リリースが面倒になる

それぞれ別管理すると、バージョンの不整合や依存関係の問題が発生しやすい

---

言語が統一されたモノレポは、依存関係の問題が少なくなり便利

---

じゃあ言語が統一できない場合は？

今回だと、フロントエンドはTypescript、バックエンドはScala

---

### sbtで全部管理しよう！

---

sbtは独自のコマンドを追加したりできる

具体的にはpnpmという名前のタスクを追加して、フロントエンドの管理をsbtで行う

```scala
lazy val pnpm = inputKey[Unit]("pnpm command with arguments")
```

---

追加したタスクにpnpmのコマンドを実行する処理を追加

- ユーザー引数をスペースで区切り、それを一つの文字列に結合
- 実行するpnpmコマンドの作成
- pnpmコマンドを実行
- タスク実行情報をログに書き出す
- エラーが発生した場合は停止

```scala
pnpm := {
  val taskName = spaceDelimited("<arg>").parsed.mkString(" ")
  pnpmInstall.value
  val localPnpmCommand = "pnpm " + taskName
  def runPnpmTask(): Int = Process(localPnpmCommand, uiDirectory.value).!
  streams.value.log("Running pnpm task: " + taskName)
  haltOnCmdResultError(runPnpmTask())
}
```

---

![](./image/image1.png)

---

### 嬉しさ

- ScalaとSvelteのコードを一緒に管理できる
- Compile時にScalaとSvelteのコードをまとめて型チェックできる
- Test実行でScalaとSvelteのテストをまとめて実行できる
- Run実行でScalaとSvelteのサーバーをまとめて起動できる

つまり CI/CDをそれぞれ用意する必要がない

先ほどのAPIの型安全性もこれでまとめて検知できる

---

## フロントエンドの可観測性

---

フロントエンドも計装はOpenTelemetryで行う

Nodeは`@opentelemetry/xxx`のパッケージを複数組み合わせて使う

バックエンドと同じように`OpenTelemetry`を使用して計装を行い、`OpenTelemetry Collector`を使用してデータを収集する

`OpenTelemetry Collector`から`Prometheus`, `Jaeger`にデータを送信し、`Grafana`にデータを集約する

![](./image/image2.png)

---

[@opentelemetry/sdk-trace-node](https://github.com/open-telemetry/opentelemetry-js/tree/main/packages/opentelemetry-sdk-trace-node)を使用した自動計装の設定

- トレース/メトリクス情報を`OpenTelemetry Collector`に送信
- プラグインを使用してHttpリクエストの計装/Expressの計装を行う
- `sdk.start()`で計装を開始

```javascript
const sdk = new NodeSDK({
  serviceName: 'management-tool-ui',
  traceExporter: new OTLPTraceExporter({
    url: process.env.OTEL_COLLECTOR_URL || 'http://otel-collector:4317'
  }),
  metricReader: new PeriodicExportingMetricReader({
    exporter: new OTLPMetricExporter({
      url: process.env.OTEL_COLLECTOR_URL || 'http://otel-collector:4317'
    }),
  }),
  instrumentations: [
    getNodeAutoInstrumentations(),
    new HttpInstrumentation(),
    new ExpressInstrumentation()
  ],
})

sdk.start()
```

---

`Dockerfile`の設定で、起動時に`instrumentation.cjs`を読み込むようにする

```Dockefile
CMD ["node", "--require", "./instrumentation.cjs", "./server.js"]
```

---

![](./image/image3.png)

---
