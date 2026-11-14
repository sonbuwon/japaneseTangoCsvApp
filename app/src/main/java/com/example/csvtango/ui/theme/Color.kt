package com.example.csvtango.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * 어두운 리더 배색.
 * 바탕 #15161a, 바 그라데이션 #2b2f37 → #363a42, 본문 #cfd5df,
 * 강조는 본문 글자와 같은 계열의 하얀 회색(#cfd5df), 북마크는 문법 형광펜 노랑(#ffd65a).
 */

val Paper = Color(0xFF15161A)          // 화면 바탕
val Sheet = Color(0xFF2B2F37)          // 카드·바 표면
val Ink = Color(0xFFCFD5DF)            // 본문 글자
val Muted = Color(0xFF8B929E)          // 보조 글자
val Line = Color(0xFF3E434C)           // 테두리·구분선
val Accent = Color(0xFFCFD5DF)         // 강조 (본문 글자 계열 하얀 회색)
val AccentSoft = Color(0xFF3A3F48)     // 강조 옅은 바탕
val AccentLine = Color(0xFF6A717D)
val FieldBg = Color(0xFF23262D)        // 입력칸·칩 바탕
val Hover = Color(0xFF363A42)

/** 강조색(하얀 회색·초록) 위에 올리는 글자색. */
val OnAccent = Color(0xFF15161A)

val Gold = Color(0xFFFFD65A)           // 북마크 (노랑 형광펜)
val GoldSoft = Color(0xFF4A4535)
val GoldLine = Color(0xFF5E5640)
val GoldBg = Color(0xFF2E2D2B)
val StarOff = Color(0xFF5A606B)

/** 현재 위치 표시 (주황). */
val Marker = Color(0xFFFFB84D)

val OkBg = Color(0xFF1E3A2D)
val OkInk = Color(0xFF7FD8A6)
val ErrBg = Color(0xFF3F2427)
val ErrInk = Color(0xFFFF8A8A)

/** 편집 팝업·목록 뒤에 까는 어두운 막. */
val Scrim = Color(0x8C000000)          // rgba(0,0,0,.55)

/** 엑셀을 대표하는 초록. 어두운 바탕에서 보이도록 밝게 올렸다. [+ csv] 버튼에 쓴다. */
val ExcelGreen = Color(0xFF5CC98A)

/** 상단 타이틀바 그라데이션. */
val TopBarBrush = Brush.verticalGradient(listOf(Color(0xFF2B2F37), Color(0xFF363A42)))

/** 하단 바 그라데이션 (아래로 갈수록 #2b2f37). */
val BottomBarBrush = Brush.verticalGradient(listOf(Color(0xFF363A42), Color(0xFF2B2F37)))

/** 진행 막대 손잡이를 끌 때 두르는 포커스 링. rgba(170,178,192,.25) */
val KnobRing = Color(0x40AAB2C0)
