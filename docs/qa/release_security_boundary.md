# 얼굴 생성과 배포 보안 경계

2026-11-19 리팩토링 P0. 현재 앱은 온라인 얼굴 생성 운영 서버가 연결되지 않은 오프라인 빌드다. Fake 응답은 실제 얼굴 생성 성공으로 표시하지 않는다.

## 적용한 경계

- `local.properties`의 공급자 키와 모델을 BuildConfig에 넣던 코드를 제거했다. 개발·배포 빌드 모두 서비스 비밀키를 빌드 설정에서 읽지 않는다.
- 공급자 직접 호출 실험인 `OpenAiFaceSheetClient`는 `src/debug`에만 존재한다. 앱의 클라이언트 선택에서는 사용하지 않으며 release APK에 포함되지 않는다.
- 현재 main/release manifest에는 INTERNET 권한이 없다. debug에만 개발용 네트워크 권한이 있다. cleartext traffic은 허용하지 않는다.
- `FaceSheetClients.onlineClient()`는 중계 서비스가 준비되기 전까지 null을 반환한다. 기존 Fake의 성공·실패·지연·취소 경로와 기본 캐릭터 진행은 유지한다.
- 사진 안내에는 현재 외부 전송이 없으며 기본 외형을 사용한다는 사실을 표시한다. 사진 지우기는 시작 전 선택 참조와 사용하지 않는 생성 파일을 정리한다. 이미 완료한 인생의 기록을 일괄 삭제하는 기능으로 표현하지 않는다.

## 로컬 저장과 자동 백업

Android 11 이하와 Android 12 이상의 cloud backup·device transfer 모두 `lifegame_ui_prefs.xml`만 허용한다. 사진 파일, 생성 얼굴 시트, 사진 참조가 포함될 수 있는 현재/완료 인생 저장은 자동 백업에서 제외한다. 기존 기기 안 저장·이어하기·완료 인생은 그대로 유지된다.

이 정책은 기기 교체나 재설치 시 인생 기록 복원을 보장하지 않는다. 기록 내 사진 정보를 분리·정제한 내보내기 또는 명시적인 동의형 백업을 추가할 때 별도 계약과 복원 시험이 필요하다.

## 온라인 생성 활성화의 선행조건

운영 주체·예산이 정해지고 실제 중계 서버가 제공된 뒤 다음을 함께 구현한다.

1. 서비스 비밀키는 서버에만 보관하고 앱은 사용자에게 발급한 제한된 인증 세션을 사용한다.
2. 인증, 요청 횟수·용량 제한, 시간 초과, 취소, 오류 응답 계약을 정의한다.
3. 업로드 대상·목적·보관·삭제 정책을 사용자 안내와 일치시킨다.
4. 만료·거부·네트워크 실패·취소 후에도 기본 캐릭터로 진행하는 테스트를 통과한다.
5. 실제 연결을 검증한 변경에서만 온라인 클라이언트와 release 네트워크 권한을 활성화한다.

이번 작업은 서버를 배포하거나 실제 공급자 호출을 실행하지 않는다.

## 재현 명령

```powershell
python scripts/verify_release_security.py
python -m unittest discover -s scripts/tests -p test_release_security.py -v
.\gradlew.bat testDebugUnitTest testReleaseUnitTest assembleRelease
python scripts/verify_release_security.py --apk app/build/outputs/apk/release/app-release-unsigned.apk
```

검사기는 소스·manifest·백업 규칙과 APK의 DEX/manifest를 검사한다. 실제 `local.properties`의 값은 읽지 않으며, 검출한 문자열이나 비밀값을 출력하지 않는다. 합성 키·sentinel·직접 클라이언트·INTERNET 권한을 넣은 가짜 APK가 거부되는 회귀 테스트를 포함한다.

근거: [OpenAI API 키 보안](https://help.openai.com/en/articles/5112595-best-practices-for-api-key-safety), [Android Auto Backup](https://developer.android.com/identity/data/autobackup).

## 검증본 버전·서명·런처 아이콘

이번 검증본은 `versionCode=2`, `versionName=0.2.0-dev`다. 기존 `com.example.lifegame` applicationId와 저장소 이름·저장 형식을 유지한다. 같은 서명으로 업데이트 설치해야 기존 기기 저장을 이어받으며, 재설치·데이터 삭제로 업데이트 검증을 대신하지 않는다. 이후 배포본의 versionCode는 이전 배포보다 높여 중복·다운그레이드를 피한다. `-dev` 표기는 미출시 검증본이며 최종 인수나 실서비스 연결 완료를 뜻하지 않는다.

Release APK는 배포키 설정이 없는 unsigned 산출물이다. 이번 변경에서 서명 키·환경 비밀값을 읽거나 새 서명·배포를 수행하지 않는다. 실제 배포 시에는 같은 앱의 기존 서명 정체성을 유지하고 최종 인수 결과·APK 체크섬·알려진 제한을 함께 확인한다.

런처는 주사위에서 세 갈래 인생 경로가 뻗는 벡터 심벌이다. 108dp 레이어의 핵심 그림을 중앙 66dp 안에 두고 기존 종이색·어두운 녹색·금색을 사용한다. API 26~32에는 전경·배경을 분리한 adaptive icon, API 33 이상에는 같은 심벌의 monochrome 레이어를 제공한다. 테마 아이콘을 지원하지 않거나 사용자가 끈 경우 컬러 아이콘으로 표시된다. 기본 정적 벡터도 보존하며, 현재 minSdk는 26이다. 런처 모양 변경은 앱 화면 그림이나 저장 내용을 바꾸지 않는다.

근거: [Android adaptive icon 규격](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [앱 버전 관리](https://developer.android.com/studio/publish/versioning).
