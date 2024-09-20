---
theme: seriph
title: Svelte 4から5でのコンポーネント開発
download: false
lineNumbers: true
background: https://source.unsplash.com/collection/94734566/1920x1080
class: 'text-center'
---

# Svelte 4から5でのコンポーネント開発

2024年9月20日 (金)

株式会社 Nextbeat
富永 孝彦

---

# はじめに
1年ぐらい前？にSvelte 5の機能を紹介したが、そこから色々変わってるので更新の意味を込めて再度紹介する

Svelte 5はまだ開発中のバージョン。現時点で`5.0.0-next.247`まで開発が進んでいます。(どこまでいくんだ〜)

予定では2024年リリースだったかと思うが結構遅れてるらしい。(毎日リリースしているのに...)

今回はこのSvelte 5で導入される新機能と変更された機能を使用して、Svelte 4までのコンポーネント開発からどのように変わるかを見ていきます。

※ Svelte 5はまだリリースされてないので、まだ仕様や機能が変更される可能性があります。

---

# Svelte 5の新機能

Svelte 5で導入される新機能は主に[Rune](https://svelte.jp/blog/runes)がメインとなる。

Runeで提供されている機能は以下の通り。今回はこのRuneを使用したコンポーネント開発を見ていきます。

- `$state{.frozen|.snapshot}`
- `$derived{.by}`
- `$effect{.pre|.active|.root}`
- `$props`
- `$bindable`
- `$inspect`
- `$host`

それぞれの詳細な使用方法は[Runeのドキュメント](https://svelte-5-preview.vercel.app/docs/runes)を参照してください。

※ Svelte 5になってもSvelte 4までの作り方で引き続きコンポーネントを作成することも可能です。(一部機能を除いて)

---

## Svelte 5 非推奨機能

Runeとは直接的には関係ないがSvelte 5で非推奨となる機能

- `on:{event}`
- `createEventDispatcher`
- `$$Generic`
- internalパッケージ (厳密には4から)
- etc…

---

## Rune設定

Runeを使う場合現時点ではアプリケーション全体で指定する方法とコンポーネント単位で指定する方法がある

Runeを有効にするとそのコンポーネントでは、先ほどあげた非推奨機能は全てエラーとなる

触っている感じ、未リリースかつSvelteKitなどその他ライブラリが対応していない現状だとコンポーネント単位で有効化するのが良さそう

---

### Rune設定 (アプリケーション全体)

アプリケーション全体で設定する場合は、`svelte.config.js`に以下設定を追加

```js
export default {
  compilerOptions: {
    runes: true
  }
}
```

---

### Rune設定 (コンポーネント単位)

コンポーネント単位で設定する場合は以下オプションを`.svelte`ファイルの頭に設定する

```svelte
<svelte:options runes={true} />
```

使う場合はtrue, 使わない場合はfalse (記載しない場合でも使用しないことになる)

---

# Svelte 4までのコンポーネント開発

まずはSvelte 4までのコンポーネント開発を見ていきます。

よく作る`Button.svelte`を例に見ていきます。

コンポーネント側で受け取る引数は以下のように実装していたかと思います。

````md magic-move {lines: true}
```svelte
<script lang="ts" context="module">
  export type Props = {
    type: 'button' | 'submit'
    color: 'primary' | 'secondary'
    disabled?: boolean
    width?: string
    height?: string
    class?: string
  }
</script>

<script lang="ts">
  export let props: Props
</script>
```

```svelte
<script lang="ts">
  export let type: 'button' | 'submit' = 'button'
  export let color: 'primary' | 'secondary' = 'primary'
  export let disabled: boolean = false
  export let width: string = 'auto'
  export let height: string = 'auto'
  export let clazz: string | undefined
</script>
```
````
---

あとはイベント含め`<button>`タグにそれぞれの引数をバインドしてコンポーネントを作成していたかと思います。

```svelte
<script lang="ts">
  import { createEventDispatcher } from 'svelte'

  export let props: Props

  const dispatch = createEventDispatcher()

  const width = props?.width ?? 'auto'
  const height = props?.height ?? 'auto'
  const type = props.type ?? 'button'
</script>

<button
  {type}
  disabled={props.disabled ?? false}
  on:click={() => dispatch('click')}
  style:width
  style:height
  class={ `${ props.color } ${props.class ?? ''}` }
>
  <slot/>
</button>
```

---

使用する時はこんな感じ

```svelte
<script lang="ts">
  import Button, { type Props } from '$views/common/component/Button.svelte'
 
  const props: Props = {
    type: 'button',
    color: 'primary',
    class: 'test',
    disabled: true,
  }
</script>

<Button {props} on:click={() => console.log('Clicked!!')}>
  Click me
</Button>
```

---

# Svelte 5でのコンポーネント開発

Svelte 5でのコンポーネント開発を見ていきます。

````md magic-move {lines: true}
```svelte
<script lang="ts">
  import { createEventDispatcher } from 'svelte'

  export let props: Props

  const dispatch = createEventDispatcher()

  const width = props?.width ?? 'auto'
  const height = props?.height ?? 'auto'
  const type = props.type ?? 'button'
</script>

<button
  {type}
  disabled={props.disabled ?? false}
  on:click={() => dispatch('click')}
  style:width
  style:height
  class={ `${ props.color } ${props.class ?? ''}` }
>
  <slot/>
</button>
```

```svelte {*|1|6-11|4|13-15|18|19-21|*}
<svelte:options runes={true} />

<script lang="ts">
  import type { HTMLButtonAttributes } from 'svelte/elements'

  // 従来の Svelte における export let や $$props, $$restProps に相当
  let {
   color,
   children,
   ...restProps // 従来の Svelte における $$restProps に相当
  }: { color: 'primary' | 'secondary' } & HTMLButtonAttributes = $props()

  const propsWeControl = $derived({ // 従来の Svelte における $: に相当
    class: color + (restProps.class ? ' ' + restProps.class : '') // colorをclassとして使うため上書きされないようにする
  })
</script>

<button {...{ ...restProps, ...propsWeControl }}>
  {#if children}
    {@render children()} // 従来の Svelte における slot に相当
  {/if}
</button>
```
````
---

````md magic-move {lines: true}
```svelte
<script lang="ts">
  import Button, { type Props } from '$views/common/component/Button.svelte'
 
  const props: Props = {
    type: 'button',
    color: 'primary',
    class: 'test',
    disabled: true,
  }
</script>

<Button {props} on:click={() => console.log('Clicked!!')}>
  Click me
</Button>
```

```svelte {*|5-11|2|6,9|8,10|14|*}
<script lang="ts">
  import { type ComponentProps } from 'svelte'
  import Button from '$views/common/component/Button.svelte'
 
  const props: ComponentProps<Button> = {
    type: 'button',
    color: 'primary',
    class: 'test',
    disabled: true,
    style: 'width: 100px; height: 40px',
  }
</script>

<Button {...props} onclick={() => console.log('Clicked!!')}> // イベントはrestPropsに含まれる
  Click me
</Button>
```
````

---

イベントを自由に渡せるようになったけど、Google Tag Managerなど外部サービスとの連携処理は共通で定義したい場合

````md magic-move {lines: true}
```svelte
<script lang="ts">
  import type { HTMLButtonAttributes } from 'svelte/elements'

  let {
   color,
   children,
   ...restProps
  }: { color: 'primary' | 'secondary' } & HTMLButtonAttributes = $props()

  const propsWeControl = $derived({
    class: color + (restProps.class ? ' ' + restProps.class : '')
  })
</script>
```

```svelte {*|6|11-14,17|*}
<script lang="ts">
  import type { HTMLButtonAttributes, MouseEventHandler } from 'svelte/elements'

  let { color, onclick, children, ...restProps }: {
   color: 'primary' | 'secondary',
   onclick: MouseEventHandler<HTMLButtonElement>
  } & HTMLButtonAttributes = $props()

  const propsWeControl = $derived({
    class: color + (restProps.class ? ' ' + restProps.class : ''),
    onclick: (event: MouseEvent & {  currentTarget: EventTarget & HTMLButtonElement }) => {
      gtm()
      onclick(event)
    }
  })

  const gtm = () => console.log('GTM')
</script>
```
````

---

Svelte 5でのコンポーネント開発はSvelte 4までと比べてコンパクトになった気がします。(型が増えて複雑になったと感じる人もいるかも)

中途半端？にマテリアルデザインのようなフレームワークを使うよりも自作した方がカスタマイズ性も簡単に維持できるので、ありかなあと最近は思う。

ただし、`useState`や`ref`のような特別な関数の使用方法を学習する必要なくシンプルな構文のみを使用してリアクティビティを実現できていた時と比べると、
`Rune`を使用することで他のフレームワーク達に近づき用途に応じて使い分けが発生するようになりました。 ここがユーザーにどう受け入れられるかが気になりますね。

あとはSvelte 5への更新が困難になったエコシステムをどう救済するのかが課題ですね。(OSSを作る上では割と必須機能が削除された。)

※ Svelte 5の構文はLintやIDEなどほとんどが対応されたいないから今現時点の開発は辛い

---

## まとめ

- イベントは`on:{event}`から`on{event}`に変更
  - `createEventDispatcher`を使用しなくてもイベントはそのまま使える
- 引数の型を明示する必要がなくなった
  - 引数に手を加えたら型も自動的に更新される
  - 必須のものだけ明示すれば良くカスタマイズ性が上がった
- `svelte:element`と組み合わせたら色々できる。
  - 例えば、単なるボタンと`a`タグを使用したリンク用のボタンをまとめたり
- TailwindCSSとの相性が良かったりする
  - 今後Styleを親で記載して子コンポーネント反映させれるようになるらしい？ので期待
