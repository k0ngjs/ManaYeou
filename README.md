<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="100" />
  <h1>ManaYeou</h1>
  <p>Android 웹툰 뷰어 앱</p>

  ![Android](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)
  ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)
  ![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)
  ![Material3](https://img.shields.io/badge/Material_3-757575?style=flat&logo=material-design&logoColor=white)
</div>

---

## 기능

- **서버 주소 자동 탐색** — 텔레그램 공지에서 최신 주소를 자동으로 가져옴
- **Cloudflare CAPTCHA 인증** — WebView 기반 CF 인증 + 숫자 인증 지원
- **뷰어** — 스크롤 / 페이지 두 가지 모드, RTL 지원, 2페이지 보기
- **마지막 페이지 저장** — 에피소드 재진입 시 마지막으로 본 페이지로 자동 이동
- **북마크 · 최근 본 만화** — 목록 관리 및 선택 삭제
- **데이터 백업** — `.yeou` 형식 내보내기/가져오기, TSV 가져오기 지원
- **테마** — 시스템 · 라이트 · 다크 선택

---

## 기술 스택

| 분류 | 라이브러리 |
|------|-----------|
| 언어 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 네트워크 | OkHttp |
| HTML 파싱 | Jsoup |
| 이미지 로딩 | Coil |
| 데이터 저장 | DataStore Preferences |

---

## 빌드

1. Android Studio에서 프로젝트 열기
2. `app/keystore.properties` 생성 후 서명 정보 입력
3. `Build > Generate Signed App Bundle / APK`

---

## 다운로드

[Releases](https://github.com/k0ngjs/ManaYeou/releases) 페이지에서 최신 APK를 받을 수 있습니다.
