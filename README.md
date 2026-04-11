<div align="center">

<img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.webp" width="120" />

# ManaYeou

Android 웹툰 뷰어 앱

[![Release](https://img.shields.io/github/v/release/k0ngjs/ManaYeou?style=flat)](https://github.com/k0ngjs/ManaYeou/releases)
[![Downloads](https://img.shields.io/github/downloads/k0ngjs/ManaYeou/total?style=flat)](https://github.com/k0ngjs/ManaYeou/releases)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)](https://github.com/k0ngjs/ManaYeou/releases)

</div>

---

## Overview

Kotlin + Jetpack Compose로 만든 Android 웹툰 뷰어입니다.
텔레그램 공지에서 서버 주소를 자동으로 가져오며, Cloudflare 인증을 지원합니다.

- **Min SDK** — Android 8.0 (API 26)
- **Language** — Kotlin
- **UI** — Jetpack Compose + Material 3

---

## Features

**서버 연결**
- 텔레그램 공지에서 최신 서버 주소 자동 탐색
- Cloudflare CAPTCHA 인증 지원
- 수동 주소 입력 지원

**뷰어**
- 스크롤 / 페이지 두 가지 보기 모드
- 페이지 방향 설정 (좌→우 / 우→좌)
- 2페이지 보기 모드
- 마지막으로 본 페이지 자동 복원

**라이브러리**
- 북마크 저장 및 관리
- 최근 본 만화 목록 (최대 20개)
- 데이터 백업 · 가져오기 (`.yeou`, TSV 형식 지원)

**기타**
- 라이트 / 다크 / 시스템 테마

---

## Download

[Releases](https://github.com/k0ngjs/ManaYeou/releases) 페이지에서 최신 APK를 받을 수 있습니다.
