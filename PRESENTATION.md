# StreamHub Android — Complete Project Presentation Guide

---

## 1. Project Overview (Ek Line Mein)

> **StreamHub** ek Android app hai jo ek hi jagah pe Indian news websites, YouTube videos, sports, tech, aur entertainment ka content dikhata hai — aur upar se **Gemini AI** se kisi bhi news ka instant bullet-point summary milta hai.

---

## 2. App Ki Features

| Feature | Description |
|---|---|
| Live Feed | YouTube + 15+ Indian news sources ek saath |
| Category Filter | All / Videos / Live / News / Podcasts / Gaming / Tech / Sports / Music |
| Search | In-memory search — title, source, description sab mein |
| Bookmarks | Room database mein permanently save hote hain |
| AI Summary (TL;DR) | Gemini 2.0 Flash se 3-5 bullet mein summary |
| Stream View | WebView mein content open + fullscreen video support |
| Custom Feed | User apni koi bhi RSS feed add kar sakta hai Settings se |

---

## 3. Data Kahan Se Aata Hai?

### Source 1 — YouTube Data API v3
```
URL: https://www.googleapis.com/youtube/v3/
```
- Trending videos India (music, news, tech, sports, gaming)
- Live Streams
- Specific YouTube Channels
- **API Key chahiye:** `YOUTUBE_API_KEY` → `local.properties` file mein

### Source 2 — RSS Feeds (20+ Indian Sources)

| Category | Sources |
|---|---|
| News | NDTV, Times of India, Hindustan Times, India Today, The Hindu, Economic Times |
| Tech | Gadgets360, Digit India, 91mobiles, TechPP |
| Sports | ESPNCricinfo, Khel Now, CricTracker |
| Entertainment | Bollywood Hungama, Pinkvilla, Koimoi |

**RSS kya hota hai?** — Har badi news website ek XML file publish karti hai apne articles ki. App vo XML download karke parse karta hai — bilkul ek reader ki tarah.

---

## 4. Complete Data Flow (Data Ka Safar — Step by Step)

```
App Launch
    │
    ▼
StreamHubApp.kt          ← Puri app ka starting point
(Database, Repository,     Sab objects yahan create hote hain
 Gemini Summarizer
 initialize karta hai)
    │
    ▼
SharedViewModel.kt        ← init{} block mein turant loadFeed() call hota hai
    │
    ▼
FeedRepository.kt         ← Decide karta hai: YouTube ya RSS?
    │
    ├──[YouTube feed]──► YouTubeDataSource.kt ──► YouTube API HTTP call ──► JSON response
    │
    └──[RSS/News feed]──► RssFeedParser.kt ──► RSS URL se XML download ──► XML parse
    │
    ▼
ContentItem (Common Format)   ← Dono sources ka data ek hi format mein convert ho jaata hai
    │
    ▼
5-5 parallel fetch (chunked) → sort by date → duplicates remove → bookmark status merge
    │
    ▼
SharedViewModel._feedItems (LiveData update)
    │
    ▼
HomeFragment.kt           ← Observer — nayi list milti hai
    │
    ▼
FeedAdapter.kt            ← RecyclerView mein cards banata hai
    │
    ▼
User card click karta hai
    │
    ▼
StreamActivity.kt         ← WebView mein content open
```

---

## 5. File-by-File Explanation (Har File Kya Karti Hai, Kaise Karti Hai)

---

### `StreamHubApp.kt`
**Kya karta hai:** Android Application class hai — app launch hone pe sabse pehle yahi run hota hai.

**Kaise karta hai:**
- `AppDatabase`, `FeedRepository`, aur `GeminiSummarizer` — teeno objects yahan `lazy` ke saath banaye hain (matlab jab pehli baar use ho tab hi create ho)
- `applicationScope` — ek CoroutineScope hai jo poori app ke saath jinda rehta hai; background tasks isme run hote hain
- Singleton pattern use kiya hai taaki har jagah se `StreamHubApp.instance` se access mile

```kotlin
val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
val repository: FeedRepository by lazy { FeedRepository(database.bookmarkDao(), applicationScope) }
val summarizer: GeminiSummarizer by lazy { GeminiSummarizer() }
```

---

### `ContentItem.kt`
**Kya karta hai:** Puri app ka **core data model** hai. Har ek card — chahe YouTube video ho, NDTV article ho, ya cricket news ho — `ContentItem` format mein hota hai.

**Kaise karta hai:**
- `data class ContentItem` — ek item ki saari info: title, thumbnail URL, source URL, category, type, publish time, views, duration, bookmark status
- `enum class FeedSource` — YOUTUBE, RSS, NEWS, PODCAST, TWITCH, CUSTOM
- `enum class ContentType` — VIDEO, LIVE, ARTICLE, AUDIO
- `enum class Category` — ALL, VIDEOS, LIVE, NEWS, PODCASTS, GAMING, TECH, SPORTS, MUSIC
- `object DefaultFeeds` — 20+ Indian sources ki hardcoded list yahan hai jo app pehli baar load hone pe use karta hai

---

### `FeedRepository.kt`
**Kya karta hai:** Data fetching ka **orchestrator** hai — YouTube aur RSS dono ko manage karta hai, bookmarks ke saath merge karta hai, aur ViewModel ko result deta hai.

**Kaise karta hai:**
- `fetchAllFeeds()` — active feeds filter karta hai, phir `chunked(5)` se 5-5 feeds parallel fetch karta hai (`async/awaitAll`)
- YouTube feed detect karna: `if (config.source == FeedSource.YOUTUBE) youtube.fetch(config) else parser.parse(config)`
- Saare results merge hone ke baad: `distinctBy { it.sourceUrl }` se duplicates hatao, `sortedByDescending { it.publishedAt }` se nayi news pehle aaye
- Room DB se bookmark IDs laata hai aur har item mein `isBookmarked` set karta hai
- `searchFeed()` — in-memory cache mein search karta hai (title, description, source, tags)
- `toggleBookmark()` — DB mein insert ya delete karta hai
- `_feedCache` — ek in-memory StateFlow cache hai jo app restart hone pe clear ho jaata hai

---

### `RssFeedParser.kt`
**Kya karta hai:** News websites ka RSS/Atom XML download karke parse karta hai aur `ContentItem` list return karta hai.

**Kaise karta hai:**
1. **Download:** OkHttp library se HTTP GET request bhejta hai RSS URL pe
   - Custom User-Agent set karta hai: `"StreamHub/1.0 (Android RSS Reader)"` taaki websites block na karein
   - 15 sec connect timeout, 20 sec read timeout
2. **Sanitize:** Kuch websites ke XML mein bare `&` hote hain jo valid XML nahi hai — regex se fix karta hai
3. **Parse:** Android ka built-in `XmlPullParser` use karta hai (koi external library nahi)
   - RSS format: `<item>` tags
   - Atom format: `<entry>` tags
   - Media thumbnails: `<media:content>`, `<media:thumbnail>`, `<enclosure>` tags
4. Per feed maximum **30 items** return karta hai
5. Date formats: 6 alag formats support karta hai (RSS aur Atom dono ke)

---

### `YouTubeDataSource.kt`
**Kya karta hai:** YouTube Data API v3 se videos fetch karta hai — trending, live, channel.

**Kaise karta hai:**
- **Custom URL scheme** use karta hai config mein:
  - `yt://trending` → India ke popular videos
  - `yt://trending/gaming` → Gaming category (categoryId = "20")
  - `yt://live` → Live streams
  - `yt://channel/CHANNEL_ID` → Specific channel ke videos
- **Retrofit** se YouTube API call karta hai
- API Key: `BuildConfig.YOUTUBE_API_KEY` (local.properties se build time pe inject hota hai)
- ISO 8601 duration parse karta hai: `PT1H23M45S` → seconds (Pattern regex se)
- YouTube category IDs: music=10, sports=17, gaming=20, news=25, tech=28

---

### `YouTubeApiService.kt`
**Kya karta hai:** Retrofit interface — YouTube API ke HTTP endpoints define karta hai.

**Kaise karta hai:**
- `@GET` annotations se endpoints define hain
- `getTrendingVideos()` → `videos?chart=mostPopular&regionCode=IN`
- `getLiveStreams()` → `search?type=video&eventType=live`
- `getChannelVideos()` → `search?channelId=...`

---

### `YouTubeModels.kt`
**Kya karta hai:** YouTube API ka JSON response parse karne ke liye data classes hain (Gson se automatically map hote hain).

**Classes:**
- `YouTubeVideoListResponse` → videos.list API ka response
- `YouTubeVideoItem` → ek video (id, snippet, statistics, contentDetails)
- `VideoThumbnails` → `best()` function — maxres → high → medium → default priority order mein best thumbnail deta hai
- `VideoStatistics` → viewCount, likeCount
- `YouTubeSearchResponse` → search.list API ka response (live aur channel ke liye)

---

### `AppDatabase.kt`
**Kya karta hai:** Room database — sirf **bookmarks** permanently phone pe store hote hain.

**Kaise karta hai:**
- `@Entity` — `BookmarkEntity` table ka structure define karta hai (`bookmarks` table)
- `@Dao` — `BookmarkDao` interface — SQL queries define hain:
  - `getAllBookmarks()` → `Flow<List<BookmarkEntity>>` — real-time updates milte hain
  - `getAllBookmarkIds()` → sirf IDs (feed mein star dikhane ke liye)
  - `isBookmarked(id)` → boolean
  - `insert()`, `deleteById()`, `deleteAll()`
- `AppDatabase` → singleton Room instance
- Extension functions: `ContentItem.toEntity()` aur `BookmarkEntity.toDomain()` — dono formats ke beech convert karte hain

---

### `SharedViewModel.kt`
**Kya karta hai:** Puri app ka **UI brain** — MainActivity ke saath create hota hai aur sabhi fragments isko share karte hain. Feed state, bookmarks, search, aur AI summaries sab yahan manage hote hain.

**Kaise karta hai:**
- `init {}` block mein `loadFeed()` call hota hai — app open hote hi data fetch shuru
- `loadFeed(category)` → Repository ko call karta hai, `_feedItems` LiveData update karta hai
- `setCategory(cat)` → category change hone par nayi feed load karta hai
- `search(query)` → repository ke in-memory cache mein search karta hai
- `summarizeItem(item)` → GeminiSummarizer ko call karta hai, `_summaries` StateFlow update karta hai
- `toggleBookmark(item)` → repository se DB operation karta hai
- `bookmarkIds` → Room ka Flow, StateFlow mein convert — har card mein star icon update karne ke liye
- `feedSources` → user ki active/inactive feeds manage karta hai; `addFeedSource()`, `removeFeedSource()`, `toggleFeedSource()` functions hain

---

### `HomeFragment.kt`
**Kya karta hai:** Main screen — feed cards dikhata hai, category filter, search bar, pull-to-refresh.

**Kaise karta hai:**
- `activityViewModels {}` se SharedViewModel attach karta hai (MainActivity se shared)
- `setupCategoryChips()` — Category enum ke saare values se chips banata hai dynamically
- `setupSwipeRefresh()` → `viewModel.refreshFeed()` call karta hai
- `setupSearchMenu()` → ActionBar mein SearchView add karta hai — `onQueryTextChange` se real-time search
- `observeData()` → 5 observers: feedItems, isLoading, error, activeCategory, bookmarkIds, summaries
- Shimmer effect: jab loading ho aur list empty ho tab shimmer layout dikhta hai
- `openStream(item)` → StreamActivity start karta hai item pass karke

---

### `FeedAdapter.kt`
**Kya karta hai:** RecyclerView ka adapter — har ContentItem ke liye ek card banata hai.

**Kaise karta hai:**
- `ListAdapter` extend karta hai with `DiffUtil` — sirf nayi/changed items update hote hain, pura list refresh nahi hota (performance ke liye)
- **Payload-based partial updates** — bookmark ya summary change hone par sirf vo item ka vo hissa update hota hai, poora card rebind nahi hota
  - `BOOKMARK_PAYLOAD` → sirf star icon toggle
  - `SUMMARY_PAYLOAD` → sirf summary section update
- `updateSummary(state)` → `SummaryState` ke 4 cases handle karta hai:
  - `Idle` → "✨ TL;DR" button dikhao
  - `Loading` → ProgressBar dikhao, button disable
  - `Success` → bullet points text dikhao
  - `Error` → error message + "↺ Retry" button

---

### `StreamActivity.kt`
**Kya karta hai:** Content viewer — WebView mein article/video open karta hai.

**Kaise karta hai:**
- `ContentItem` Intent se receive karta hai (`Serializable`)
- **WebView setup:**
  - JavaScript enabled (YouTube videos ke liye zaruri)
  - Custom User-Agent: Pixel 7 Chrome browser ka — websites ka proper version mile
  - `mediaPlaybackRequiresUserGesture = false` → autoplay work kare
- **Ad Cleanup:** Page load hone ke baad JavaScript inject karta hai (`CLEAN_JS`) jo headers, navbars, ads, cookie banners, sidebars hide karta hai
- **Fullscreen video:** `WebChromeClient.onShowCustomView()` → system bars hide, fullscreen layout show
- **Back navigation:** Pehle WebView ka back history check karta hai, phir fullscreen exit, phir activity finish
- Buttons: Bookmark toggle, Share, Open in Browser, Reload, WebView Back/Forward

---

### `GeminiSummarizer.kt`
**Kya karta hai:** Google Gemini 2.0 Flash API se article ka bullet-point summary generate karta hai.

**Kaise karta hai:**
1. `GEMINI_API_KEY` se Gemini `Client` banata hai (lazy init)
2. Description se HTML tags strip karta hai (`Html.fromHtml`)
3. Prompt banata hai:
   ```
   "Summarize this article in 3 to 5 short bullet points.
   Each bullet must start with '•'. Plain text only, no markdown.
   
   Title: [title]
   Content: [first 2000 chars of description]"
   ```
4. `client.models.generateContent("gemini-2.0-flash", prompt)` call karta hai
5. Rate limit handle karta hai: error message mein seconds extract karke user ko batata hai "Try again in Xs"

---

### `SummaryState.kt`
**Kya karta hai:** Gemini summary ke 4 possible states define karta hai (sealed class).

```kotlin
Idle     → Summary abhi maangi nahi gayi
Loading  → API call chal rahi hai
Success  → bullets: String — actual summary text
Error    → message: String — kya galat hua
```

**Sealed class kyun?** — Ye ensure karta hai ki `when` statement mein saare cases handle hon, koi bhi state miss na ho.

---

### `BookmarksFragment.kt`
**Kya karta hai:** Saved bookmarks ki list dikhata hai Room database se.

**Kaise karta hai:**
- Wahi `FeedAdapter` reuse karta hai jo HomeFragment mein use hota hai
- `viewModel.bookmarks.observe()` → Room DB ka Flow observe karta hai — bookmark add/remove hote hi list automatically update hoti hai
- "Clear All" button → AlertDialog confirm → `viewModel.clearAllBookmarks()`
- Count dikhata hai: "5 saved"

---

### `SettingsFragment.kt`
**Kya karta hai:** Feed sources manage karne ka screen — toggle on/off, add new RSS, delete feed.

**Kaise karta hai:**
- `renderFeedList()` → har FeedConfig ke liye dynamically row inflate karta hai (switch + delete button)
- Switch toggle → `viewModel.toggleFeedSource(id)` → `FeedConfig.isActive` flip hota hai
- **Add Feed Dialog:** URL validate karta hai (`http` se shuru ho), UUID se unique ID generate karta hai, `FeedSource.RSS` as default source
- Delete → AlertDialog confirm → `viewModel.removeFeedSource(id)`

---

## 6. Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  HomeFragment  BookmarksFragment  SettingsFragment │
│  StreamActivity                                  │
└─────────────────┬───────────────────────────────┘
                  │  observe / call
┌─────────────────▼───────────────────────────────┐
│              ViewModel Layer                     │
│              SharedViewModel                     │
│  (LiveData + StateFlow — UI state hold karta hai)│
└─────────────────┬───────────────────────────────┘
                  │  suspend functions
┌─────────────────▼───────────────────────────────┐
│               Data Layer                         │
│  FeedRepository ──► RssFeedParser (RSS/XML)      │
│                 ──► YouTubeDataSource (API)      │
│                 ──► BookmarkDao (Room DB)         │
│  GeminiSummarizer ──► Gemini 2.0 Flash API       │
└─────────────────────────────────────────────────┘
```

**Pattern: MVVM (Model-View-ViewModel)**
- **View** = Fragments + Activities (sirf UI dikhana)
- **ViewModel** = SharedViewModel (business logic + state)
- **Model** = Repository + DataSources + Room DB (data)

**Kyun MVVM?**
- ViewModel screen rotation pe survive karta hai (data loss nahi hota)
- Fragment aur Activity ko directly data fetch nahi karna padta
- Testing easy hoti hai

---

## 7. Libraries Used

| Library | Kaam |
|---|---|
| **Retrofit** | YouTube API ke HTTP calls |
| **OkHttp** | RSS feed download karne ke liye |
| **Gson** | YouTube JSON response parse karna |
| **Room** | Local SQLite database (bookmarks) |
| **Gemini Android SDK** | AI summary |
| **Glide / Coil** | Thumbnail images load karna |
| **Material Components** | UI chips, cards, snackbar |
| **Kotlin Coroutines** | Background async tasks |
| **ViewBinding** | XML layouts se type-safe binding |

---

---

# VIVA QUESTIONS — Presentation Ke Liye Tayari Karo

---

## Basic Questions (Aasaan — Zarur Poochhenge)

**Q1. Aapka app kya karta hai? Ek line mein batao.**
> StreamHub ek Android content aggregator app hai jo YouTube, Indian news websites aur RSS feeds ka content ek jagah dikhata hai aur Gemini AI se news ka instant summary deta hai.

**Q2. Data kahan se aata hai?**
> Do sources se: (1) YouTube Data API v3 — trending videos aur live streams ke liye. (2) RSS Feeds — NDTV, Times of India, Gadgets360 jaisi websites apna content XML format mein publish karti hain, hum vo XML download karke parse karte hain.

**Q3. RSS kya hota hai?**
> RSS ek XML-based format hai jisme websites apne latest articles ka summary publish karti hain. Humara app us URL se XML file download karta hai, phir `XmlPullParser` se title, link, description, thumbnail extract karta hai aur `ContentItem` format mein convert karta hai.

**Q4. Architecture kaunsa use kiya hai?**
> MVVM — Model View ViewModel. View sirf UI dikhata hai, ViewModel business logic aur state hold karta hai, aur Model data laata hai (Repository + DataSources + Room DB).

**Q5. Gemini AI ka kya use hai?**
> Jab user "TL;DR" button click karta hai, `GeminiSummarizer` article ka title aur description Gemini 2.0 Flash API ko bhejta hai aur 3-5 bullet points mein summary return karta hai.

---

## Medium Questions (Thoda Deep)

**Q6. Parallel fetching kaise kiya hai?**
> `FeedRepository.fetchAllFeeds()` mein feeds ko `chunked(5)` se 5-5 ke batches mein divide karte hain, phir har batch mein `async {}` + `awaitAll()` se sab parallel fetch hote hain. Isse time save hota hai — 20 feeds ek saath fetch ho sakti hain.

**Q7. Bookmarks kaise kaam karte hain?**
> Room database use kiya hai. `BookmarkDao` se `insert()` aur `deleteById()` karte hain. `getAllBookmarks()` ek `Flow` return karta hai jo automatically update hota hai jab bhi koi change ho. Is Flow ko ViewModel `LiveData` mein convert karta hai fragments ke liye.

**Q8. Search kaise implement kiya?**
> In-memory search hai — network call nahi hoti. `FeedRepository` ek `_feedCache` StateFlow maintain karta hai. `searchFeed(query)` us cache ko filter karta hai — title, description, sourceName, aur tags mein query dhundta hai.

**Q9. DiffUtil kya hai aur kyun use kiya?**
> DiffUtil RecyclerView mein purani aur nayi list compare karta hai aur sirf changed items ko update karta hai. Isse performance better hoti hai — agar 100 items mein se 2 change hue toh sirf vo 2 animate honge, poora list refresh nahi hoga.

**Q10. Payload-based partial update kya hai? (FeedAdapter mein)**
> Jab sirf bookmark status ya summary change ho, poora card rebind karna waste hai. Isliye `notifyItemRangeChanged(0, itemCount, BOOKMARK_PAYLOAD)` call karte hain — RecyclerView sirf bookmark icon update karta hai bina baaki card touch kiye.

**Q11. ContentItem mein `Serializable` kyun implement kiya?**
> `StreamActivity` ko `ContentItem` pass karna hota hai via `Intent`. Intent ke through objects Serializable ya Parcelable format mein hi bheje ja sakte hain. Isliye `ContentItem` class `Serializable` implement karti hai.

**Q12. WebView mein JavaScript inject kyun kiya?**
> Page load hone ke baad `CLEAN_JS` inject karta hai jo ads, cookie banners, navbars, sidebars hide karta hai. Isse user ko clean reading experience milta hai bina redirect kiye ya external library use kiye.

---

## Advanced Questions (Agar Deep Dive Karen)

**Q13. `lazy` initialization ka kya matlab hai? StreamHubApp mein kyun use kiya?**
> `lazy` matlab object tab tak create nahi hoga jab tak pehli baar access na ho. Database aur Repository expensive operations hain — app startup slow na ho isliye `lazy` use kiya. Jab pehli baar `app.repository` access hoga tab hi Room database initialize hogi.

**Q14. `sealed class SummaryState` normal class se better kyun hai?**
> Sealed class ke saath compiler guarantee karta hai ki `when` expression mein saare possible states cover kiye gaye hain. Agar koi state miss ho toh compile-time error milegi. Normal class ya enum mein ye safety nahi hoti.

**Q15. `StateFlow` aur `LiveData` mein kya farq hai? Dono kyun use kiye?**
> `LiveData` lifecycle-aware hai — fragment destroy hone pe automatically stop ho jaata hai, observer leak nahi hoti. `StateFlow` Kotlin Coroutines ka part hai — always has a current value, aur non-Android code mein bhi use ho sakta hai. Humne `bookmarkIds` ke liye StateFlow use kiya kyunki adapter ko immediately current state chahiye, baaki ke liye LiveData sufficient tha.

**Q16. `chunked(5)` aur `awaitAll()` kaise parallel execution karta hai?**
> `async {}` ek `Deferred` return karta hai jo background mein run hota hai. `awaitAll()` saare Deferred complete hone ka wait karta hai. `chunked(5)` isliye ki zyada parallel requests ek saath se server rate-limit ya memory issues aa sakte hain — 5-5 ka controlled parallel fetching safe aur fast dono hai.

**Q17. Custom URL scheme `yt://` kyun banaya YouTube ke liye?**
> YouTube feeds HTTP URL nahi hain — ye API calls hain. RSS parser inhe handle nahi kar sakta. Isliye `yt://trending`, `yt://live` jaisa custom scheme banaya taaki `FeedRepository` easily decide kar sake ki is config ke liye `YouTubeDataSource.fetch()` call karna hai ya `RssFeedParser.parse()`.

**Q18. Rate limiting handle kaise kiya Gemini mein?**
> `GeminiSummarizer` ke catch block mein error message check karta hai — agar "quota" string mile toh regex se retry seconds extract karta hai (`retry in X.Xs` pattern) aur user ko exact wait time batata hai instead of generic error.

**Q19. Fullscreen video kaise implement kiya?**
> `WebChromeClient.onShowCustomView()` callback milti hai jab WebView video fullscreen request karta hai. Tab system bars hide karte hain (`WindowInsetsController`), fullscreen layout show karte hain, aur video view us layout mein add karte hain. `onHideCustomView()` pe sab reverse karte hain.

**Q20. App offline kya karta hai?**
> Pehle se loaded content `_feedCache` (in-memory StateFlow) mein rehta hai, lekin app restart hone pe clear ho jaata hai. Sirf bookmarks permanently Room database mein store hote hain — offline bhi accessible hain. Feed content ke liye internet zaruri hai.

---

## "Kya Improve Kar Sakte Ho?" Questions

**Q21. Aage kya add kar sakte ho is app mein?**
> - Local caching with Room taaki offline bhi news mile
> - Background refresh (WorkManager se scheduled sync)
> - Push notifications for breaking news
> - Dark/Light theme toggle
> - Font size settings
> - Multiple language support

**Q22. Koi limitation hai app ki?**
> - YouTube API ke liye API key chahiye — bina key ke YouTube content nahi aayega
> - Feed data app restart pe clear ho jaata hai (no offline cache)
> - Kuch websites apna RSS block karti hain toh wahan se content nahi aata
> - In-memory search sirf loaded content mein search karta hai, historical search nahi

---

## Quick Revision — 30 Second Answers

| Question | Answer |
|---|---|
| App kya hai? | Indian content aggregator — YouTube + News + AI summary |
| Data sources? | YouTube API v3 + 20+ Indian RSS feeds |
| Architecture? | MVVM — Model View ViewModel |
| Database? | Room (sirf bookmarks ke liye) |
| AI feature? | Gemini 2.0 Flash — TL;DR bullet summary |
| Parallel fetch? | chunked(5) + async/awaitAll (Kotlin Coroutines) |
| Search? | In-memory filter on cached feed |
| WebView JS inject? | Ads aur banners hide karne ke liye |
| Why sealed class? | Compile-time exhaustiveness check for SummaryState |
| Why Lazy init? | App startup fast rakhne ke liye — objects tab bane jab zarurat ho |
