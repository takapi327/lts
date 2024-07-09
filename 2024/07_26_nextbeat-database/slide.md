---
theme: seriph
title: MySQL用データベースクライアントライブラリを作成したTips
download: false
lineNumbers: true
background: https://source.unsplash.com/collection/94734566/1920x1080
class: 'text-center'
---

# 第一回データベースについて語る会

2024年7月26日 (金)

株式会社 Nextbeat
富永 孝彦

---

# はじめに

今回はデータベースのクライアント作成に関する話

MySQLのクライアント作成の時の話ですが、他のデータベースでも同じようなことが言えるかもしれません。

---

## なぜこの話を？

データベースクライアント作成に関する情報が少ない

自分が作る時情報が欲しかった

- 手順
- 便利ツール
- ドキュメント
- etc...

そういった方の参考になればと思い、この話をします。

---

# 背景と目的

クライアント自体はScalaを使用して作成しました。

Scalaは現在JVM, JS, Nativeというマルチプラットフォームに対応しています。

しかし、JDBCを使用したライブラリだとJVM環境でしか動作しません。

そのためMySQLプロトコルに対応したプレーンなScalaで書かれたコネクタを提供(こちらはまだ誠意開発中)することで、異なるプラットフォームで動作できるようにするために開発を行っています。

---

## 要は...

趣味です

---

# 開発の苦労と工夫

ここではどんな感じで開発を進めていけばいいのかわからないという方のために、参考になれば幸いです。

- 通信の監視
- パケットの見方覚える
- 答えを見える

---

## 通信の監視

なぜ通信の監視を行うと良いのか？

- パケットの見方を覚える
- 答え合わせを行いながら開発できる

---

## データベースクライアントとMySQLサーバーとの通信

データベースクライアントとMySQLサーバーとの通信は、TCP/IPを使用して行われます。

こういった通信はネットワークパケットアナライザと呼ばれる[ngrep](https://en.wikipedia.org/wiki/Ngrep)を使用して監視することで、通信内容を確認することができます。

```shell
brew install ngrep
```

※ 他にも色々なツールがあるので、自分に合ったものを選ぶと良いです。

---

# ngrepの使用方法

````md magic-move {lines: true}
```shell {*|2|*}
// step 1
sudo ngrep -x -q -d lo0 '' 'port 3306'
```

```shell {*|3|*}
// step 2
sudo ngrep -x -q -d lo0 '' 'port 3306'
Password:
```

```shell {*|4-5|*}
// step 3
sudo ngrep -x -q -d lo0 '' 'port 3306'
Password:
interface: lo0 (127.0.0.0/255.0.0.0)
filter: ( port 3306 ) and (ip || ip6)
```

```shell
// step 4 (別タブで実行)
mysql -u root -p
```

```shell {*|7-12|*}
// step 5
sudo ngrep -x -q -d lo0 '' 'port 3306'
Password:
interface: lo0 (127.0.0.0/255.0.0.0)
filter: ( port 3306 ) and (ip || ip6)

T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```
````

---

## パケットの見方

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-21.5 left-25 border-2 border-red-500 p-2 w-80"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-26 left-23 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この部分はパケットがどこからどこへ送信されたかを示しています。ここでは、MySQLサーバーからMySQLクライアントへの通信を示しています。
</p>

---

## パケットの見方

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-26 left-29 w-92 h-23 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-48 left-29 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この部分はパケットのデータ部分を示しています。ここでは、MySQLサーバーからMySQLクライアントへの通信データを示しています。
</p>

---

## パケットの見方

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-26 left-29 w-4 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-30 left-27 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  パケットは各2桁の16進数が1バイトを表しています。
</p>

---

## パケットの見方

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-26 left-29 w-21 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-30 left-27 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  MySQLでは最初の4バイトはヘッダー部分を示しており、それ以外の部分はボディのデータ部分を示しています。
</p>

<ul v-after>
  <li>ペイロードの長さ (最初の3バイト)</li>
  <li>Sequence ID (最後の1バイト)</li>
</ul>

---

## パケットの見方

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-26 left-125 w-32 h-23 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-48 left-125 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この右に表示されているデータは、パケットのデータ部分をASCII文字列で表現したものです。
</p>

---

## パケットの形式

送信されるパケットの内容はデータベースによって決まっている。

データベースごとにドキュメントが存在しているので、それを参照することでパケットの内容を理解することができる。

---

## MySQL

MySQLは、オープンソースのリレーショナルデータベース管理システム(RDBMS)です。

MySQL用のデータベースクライアントライブラリを作成する際には、MySQLの通信プロトコルについて理解することが重要です。

以下がMySQLの公式ドキュメントです。

https://dev.mysql.com/doc/dev/mysql-server/latest/

---
layout: two-cols
---

<img src="/COM_STMT_EXECUTE.png" class="h-90 rounded shadow">

::right::

これがすごくわかりづらい...

- 型の説明がない
- 説明がよくわからない
- 表に条件分岐をいれないで欲しい

初めて作る人用というよりは、知識を持っている人が確認するための資料なのかもしれない...

---

MySQLはコネクションフェーズとコマンドフェーズに分かれています。

- コネクションフェーズ
  - ハンドシェイク
  - SSL認証
  - 認証
- コマンドフェーズ
  - クエリの実行
  - プリペアドステートメントの実行
  - ストアドプロシージャの実行

---

MySQLはコネクションフェーズとコマンドフェーズに分かれています。

- コネクションフェーズ
  - ハンドシェイク
  - SSL認証
  - 認証

<div class="absolute top-25 left-19 w-45 h-30 border-2 border-red-500 p-2"></div>
<svg xmlns="http://www.w3.org/2000/svg" class="absolute top-58 left-20 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<br>

まず初めはハンドシェイクから見ていくことになる。

https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase.html

---
layout: two-cols
---

[ヘッダー](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_packets.html#sect_protocol_basic_packets_packet)

<img src="/PAYLOAD_PROTOCOL.png" class="rounded shadow">

これがハンドシェイク時にMySQLサーバーからクライアントに送信されるパケットの内容です。

::right::

[ボディ](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_connection_phase_packets_protocol_handshake_v10.html)

<img src="/HANDSHAKE_PROTOCOL.png" class="h-90 rounded shadow">

---

### 読み方

- `int<number>`
- `string<NULL>`
- `string[number]`
- `$length`

※ 他にもあるけど今日はこの4つ

---

#### 固定長整数 ([Protocol::FixedLengthInteger](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_integers.html#a_protocol_type_int1))

固定長整数は、値を固定されたバイト数で表現するためのデータ型です。

整数の値は、バイト列の中に格納されます。このバイト列はリトルエンディアン形式で格納され、最も小さいバイトが最初に来ます。

MySQLプロトコルでは、以下のような固定長の符号なし整数のバリアントがあります。

- `int<1>`: 1バイトの固定長整数
- `int<2>`: 2バイトの固定長整数
- `int<3>`: 3バイトの固定長整数
- `int<4>`: 4バイトの固定長整数
- `int<6>`: 6バイトの固定長整数
- `int<8>`: 8バイトの固定長整数

---

固定長整数の例

例えば、3バイトの固定長整数（int<3>）の場合。3バイトのデータで一つの整数を表現します。値はリトルエンディアン形式で格納されるので、以下のようになります。

| バイト順序	 | 16進数	 | 10進数 |
|--------|-------|------|
| バイト1   | 0x01  | 1    |
| バイト2   | 0x02  | 2    |
| バイト3   | 0x03  | 3    |

この場合、値は0x030201（16進数）となり、これを10進数に変換すると197121になります。

つまり、int<3>というデータ型の場合はバイト配列の内、3つのバイトで表現を行っているということです。

---

パケットに当てはめてみる

ヘッダーのドキュメントを見ると、ヘッダーは以下のようになっています。

- ペイロードの長さ: int<3>
- シーケンスID: int<1>

<img src="/PAYLOAD_PROTOCOL.png" class="rounded shadow">

---

### ペイロードの長さ: `int<3>`

つまり

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-39 left-29 w-16 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-39 left-126 w-7 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-44 left-27 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この3桁がペイロードの長さを示しています。この場合、0x4a000000を10進数に変換すると74になります。
</p>

---

### シークエンスID: `int<1>`

つまり

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-39 left-45 w-4 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-39 left-132 h-4 w-1 border-2 border-red-500 p-0.5"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-44 left-43 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この1桁がシークエンスIDを示しています。
</p>

---

#### NULL終端文字列 ([Protocol::NullTerminatedString](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_strings.html#sect_protocol_basic_dt_string_null))

MySQLプロトコルでは、文字列のデータをエンコードする際にいくつかの方法があります。その中でも、`string<NULL>`は、特定の方法で終端された文字列のことを指します。

`string<NULL>`とは、NULL文字（00バイト）で終端される文字列のことです。文字列の最後に特定のバイト（00）が付与されることで、文字列の終わりを示します。

なぜNULL終端が重要か？

NULL終端は、文字列の長さを事前に知らなくても、文字列の終わりを正確に特定できるため便利です。これにより、文字列を扱う際の処理が簡単になり、バッファオーバーフローなどのエラーを防ぎやすくなります。

---

例えば、文字列 "Hello" を`string<NULL>`としてエンコードすると以下のようになります：

| 文字列  | 16進数 |
|------|------|
| H    | 0x48 |
| e    | 0x65 |
| l    | 0x6c |
| l    | 0x6c |
| o    | 0x6f |
| NULL | 0x00 |

この場合、文字列 "Hello" は16進数で "48 65 6C 6C 6F 00" と表現されます。最後の00バイトが、文字列の終わりを示すNULL終端文字です。

---

#### 固定長文字列 ([Protocol::FixedLengthString](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_strings.html#sect_protocol_basic_dt_string_fix))

`string[number]`とは、固定長の文字列で、事前に決められた長さを持つ文字列のことです。文字列の長さは予め決まっており、これが固定されています。

例えば、`string[5]`は、5バイトの固定長文字列を表します。この場合、文字列の長さは5バイトで、文字列の長さが5バイトになるようにパディングされます。

MySQLプロトコルの中で、ERR_PacketのSQLステートは常に5バイトの長さを持つ固定長文字列です。

| 文字列データ   | 16進数表示         | 説明               |
|----------|----------------|------------------|
| "12345"  | 31 32 33 34 35 | ちょうど5バイトの文字列     |
| "AB"     | 41 42 00 00 00 | 2文字の文字列を5バイトに拡張  |

**なぜ固定長文字列が重要か？**

固定長文字列は、プロトコルの一貫性を保ち、データの読み取りと解析を容易にするために重要です。特にエラーパケットなどの固定フォーマットのデータには非常に有用です。

---

#### 可変長文字列 ([Protocol::LengthEncodedString](https://dev.mysql.com/doc/dev/mysql-server/latest/page_protocol_basic_dt_strings.html#sect_protocol_basic_dt_string_le))

`$length`とは、文字列の先頭にその文字列の長さを表すエンコードされた整数が付加される形式の文字列です。これにより、文字列の長さを事前に知ることができます。

例えば、「Hello」という文字列を長さエンコードされた文字列として表現すると以下のようになります。

1. 文字列の長さを表す整数（5）
2. 実際の文字列データ（"Hello"）

長さエンコードされた整数は、特別な形式でエンコードされます。一般的に、1バイトから最大8バイトで表現されます。

---

例えば、長さが5の文字列 "Hello" の場合：

| データ          | バイト例           |
|--------------|----------------|
| 長さ (5)       | 05             |
| 文字列 "Hello"	 | 48 65 6C 6C 6F |

つまり、"Hello" の長さエンコードされた文字列は "05 48 65 6C 6C 6F" となります。

**なぜ長さエンコードされた文字列が重要か？**

長さエンコードされた文字列は、可変長の文字列データを扱う際に便利です。文字列の先頭にその長さが含まれているため、データの読み取りが効率的になり、バッファオーバーフローなどのエラーを防ぐことができます。

---
layout: two-cols
---

パケットに当てはめてみる

<img src="/HANDSHAKE_PROTOCOL.png" class="h-90 rounded shadow">

::right::

ハンドシェイクのドキュメントを見ると以下のようになっています。

- プロトコルバージョン: `int<1>`
- サーバーバージョン: `string<NULL>`
- スレッドID: `int<4>`
- 認証プラグインのランダムデータ1: `string<8>`
- 認証プラグインのランダムデータ2: `$length`
- etc...

---

### プロトコルバージョン: `int<1>`

つまり

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-39 left-50 w-4 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-39 left-134 h-4 w-1 border-2 border-red-500 p-0.5"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-44 left-48 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この1桁がプロトコルバージョン(`int<1>`)を示しています。常に10進数の10になります。
</p>

---

### サーバーバージョン: `string<NULL>`

つまり

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-39 left-94 w-4 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-44 left-92 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  ここで0がきているので、NULL終端文字列の終わりを示しているとわかる。
</p>

---

### サーバーバージョン: `string<NULL>`

つまり

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-39 left-56 w-43 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-39 left-135 w-12 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-44 left-56 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  なので、ここまでのデータはサーバーバージョンを示しているとわかる。
</p>

---

### スレッドID: `int<4>`

つまり

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-39 left-99 w-22 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-39 left-146 w-10 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-44 left-99 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この4桁がスレッドID(`int<4>`)を示しています。
</p>

---

### 認証プラグインのランダムデータの長さ: `int<1>`

つまり

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-48 left-29 w-5 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-48 left-126 h-4 w-2 border-2 border-red-500 p-1"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-53 left-28 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この1桁が長さを示しています。15は10進数で17になります。
</p>

<p v-after>
  今回取得したいデータは、`$len=MAX(13, length of auth-plugin-data - 8)`という条件があるので、取得した長さを使い計算すると
</p>

<p v-click>
  `$len=MAX(13, 17 - 8)`
</p>

<p v-click>
  `$len=MAX(13, 9)`
</p>

<p v-click>
  `$len=13`
</p>

---

### 認証プラグインのランダムデータ: `$length`

つまり

```shell
T 127.0.0.1:3306 -> 127.0.0.1:56281 [AP] #5
  4a 00 00 00 0a 38 2e 30    2e 33 33 00 9b 19 00 00    J....8.0.33.....
  69 26 73 2a 75 08 1c 2c    00 ff ff ff 02 00 ff df    i&s*u..,.���..��
  15 00 00 00 00 00 00 00    00 00 00 63 01 02 01 2e    ...........c....
  3e 45 23 57 31 7f 5b 00    63 61 63 68 69 6e 67 5f    >E#W1.[.caching_
  73 68 61 32 5f 70 61 73    73 77 6f 72 64 00          sha2_password.
```

<div v-click class="absolute top-48 left-94 w-27 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-52 left-29 w-43 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-48 left-146 w-10 h-4 border-2 border-red-500 p-2"></div>
<div v-after class="absolute top-52 left-126 w-15 h-4 border-2 border-red-500 p-2"></div>
<svg v-after xmlns="http://www.w3.org/2000/svg" class="absolute top-58 left-28 h-8 w-8 text-red-500 transform rotate-180" viewBox="0 0 20 20" fill="currentColor">
  <path fill-rule="evenodd" d="M10 2a1 1 0 0 1 1 1v10.586l3.293-3.293a1 1 0 1 1 1.414 1.414l-5 5a1 1 0 0 1-1.414 0l-5-5a1 1 0 1 1 1.414-1.414L9 13.586V3a1 1 0 0 1 1-1z" clip-rule="evenodd" />
</svg>

<p v-after>
  この13このデータが認証プラグインのランダムデータを示しています。
</p>

<p v-after>
  ※ 間にreserved: `string[10]`があるので、その値は飛ばしています。(全て0の値)
</p>

---

## これで数字と文字列は読み取れるようになった！

読み方がわかればそんなに難しくないと思いませんか？

<p v-click>
  読めるようになってもどうやってコードに落とし込むかわからない...
</p>

<p v-click>
  何を使えばいいのかわからない...
</p>

<p v-click>
  そもそもどうやってデータを取得するのかわからない...
</p>

---

## AIを使おう！
書き出しはAIに任せる

---

### 雑に送ってみる

````md
```
4a 00 00 00
```

上記はMySQLサーバーから送られてくるパケットの一部です。それぞれ以下要件に従ってデコードするコードをScalaで作成してください。

- Scodecライブラリを使用
- 最初の3つはペイロードの長さとしてデコード
- 最後の1つはシークエンスIDとしてデコード
````

※ ライブラリの指定はしなくても良い (使いたいものがあれば指定)

---

生成されたコード

※ プロンプトや使用言語、その時の調子によって精度は変わります。

```scala
import scodec._
import scodec.bits._
import scodec.codecs._
import scodec.Attempt._

case class MySQLPacket(
  payloadLength: Int,
  sequenceId: Int
)
```

---

````md magic-move {lines: true}
```scala {*|2-4|5-8|10-12|16-22|*}
object MySQLPacketDecoder:
  // ペイロード長とシーケンスIDのコーデック定義
  val payloadLengthCodec: Codec[Int] = ("payloadLength" | uint24L)
  val sequenceIdCodec: Codec[Int] = ("sequenceId" | uint8L)
  // MySQLPacket用にコーデックを1つにまとめる
  val mySQLPacketCodec: Codec[MySQLPacket] = (
    payloadLengthCodec :: sequenceIdCodec
  ).as[MySQLPacket]
  def main(args: Array[String]): Unit =
    // 16進文字列をバイト列に変換
    val hexString = "4a000000"
    val bytesOpt = ByteVector.fromHex(hexString)
    // ByteVectorが正常に作成されたことを確認する。
    bytesOpt match
      case Some(bytes) =>
        // パケットをデコードする
        mySQLPacketCodec.decode(bytes.bits) match
          case Successful(DecodeResult(packet, _)) =>
            println(s"Payload Length: ${packet.payloadLength}")
            println(s"Sequence ID: ${packet.sequenceId}")
          case Failure(err) =>
            println(s"Failed to decode: $err")
      case None =>
        println(s"Invalid hex string: $hexString")
```
````

---

実行してみると...

```shell
Payload Length: 74
Sequence ID: 0
```

<p v-click>
  これでデータの取得ができるようになりました！
</p>

<p v-click>
  雑に投げるだけでもAIがコードを生成してくれるので、わからなくてもこれを下敷きにして進めていけばいい！
</p>


<p v-click>
  あとは同じように他の項目もデコードしていき組み合わせていけば、MySQLサーバーと通信するためのコードができていきます。
</p>

---

## コードを書いても

コードを書いたとしても本当に正しいのかわからない...

<p v-click>
リトルエンディアンとビッグエンディアンの違いで取得方法が間違っていてもデータの取得ができてしまう...
</p>

<p v-click>
間違ったデータを扱っていると、どこで間違ったのかがわからない...
</p>

<p v-click>
既にあるライブラリで答え合わせをしよう！
</p>

---

### 答え合わせとは？

MySQLのような広く使われているようなデータベースは、多くのライブラリが存在しています。

今作ろうとしているものは既に誰かが作っている。

じゃあ、それを見れば実装方法はわかるよね？

---

## 実際に答え合わせを行う

ngrepで監視できる状態ができた。パケットの見方も大体わかる。

MySQLサーバーを起動して、既存ライブラリとの通信内容を確認すればいい！

- どのような順番でデータがやり取りされているか
- どのようにエンコードしてサーバーにデータを送信すれば良いか
- etc...

私は主に以下の接続をそれぞれ監視して、どのようなデータがやり取りされているかを確認しました。

- mysql client
- mysql-connector-java

自分の作ってみたい言語のライブラリを使えばOk！

---

# 作って得たもの、良かったこと、変わったこと

---

## 得たもの

- データベースをわかった気になれる
- JDBCに詳しくなった
  - MySQLサーバーと通信していると思っていたメソッドが実は通信していなかったり
  - 自分の想定していた処理と違っていたり
  - etc...
- どんな処理でもパフォーマンスを考えるようになった (これがなんとなくわかるようになった)

---

## 変わったこと

- 何かエラーが起きたらパケットのやり取りを見にいくようになった
- データベースがシンプルに見えるようになった
- データベースのクラアインとライブラリのコードを読めるようになった (触ったことない言語でも)

---

# 今後やりたいこと

- 対応できていない機能の実装
- データベースそのものを作ってみたい

---

# データベースに関連する技術的詳細

- ネットワークプロトコル
- パケット通信
- エンディアン
- バイナリデータの扱い
- データのシリアライゼーション
- データのデシリアライゼーション
- etc...