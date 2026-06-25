# UltraSandbox

[![🇬🇧 English](https://img.shields.io/badge/🇬🇧-DO_YOU_SPEAK_IT-green.svg)](README.md)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/lang-Java-orange.svg)]()
[![Kotlin](https://img.shields.io/badge/lang-Kotlin-purple.svg)]()
[![Release](https://img.shields.io/github/v/release/alim-zanibekov/ultrasandbox)](https://github.com/alim-zanibekov/ultrasandbox/releases/latest)

ReVanced-патч, который заставляет Android-приложения думать, что они работают на чистом телефоне без
других приложений и сервисов.

## Что делает

- VPN не виден: tun/tap/wg интерфейсы скрыты, TRANSPORT_VPN возвращает false.
- WiFi/Bluetooth: пустые результаты сканирования, поддельный MAC-адрес.
- localhost: заблокированы все подключения кроме системных портов (DNS, DHCP).
- /proc/net: приложение видит только свои соединения и системные.
- /proc/self/maps: строки с Frida, Xposed, Magisk удалены.
- Root: su, Magisk, BusyBox, Xposed не обнаруживаются.
- Список приложений: видны только предустановленные системные.
- Идентификаторы: IMEI, IMSI, серийный номер SIM скрыты. Android ID случайный.
- Данные пользователя: контакты, звонки, SMS недоступны.

## Установка

### ReVanced Manager (телефон)

1. ReVanced Manager > Patches > Add patches
2. "Enter URL" > вставить:
   `https://github.com/alim-zanibekov/ultrasandbox/releases/latest/download/patches.json`
3. Patcher > выбрать приложение > включить UltraSandbox > Patch

### revanced-cli (ПК)

```
java -jar revanced-cli.jar patch \
  -p ultrasandbox-patches.rvp -b \
  --force target.apk
```

На выходе получаем подписанный APK.
Перед установкой нужно удалить оригинальное приложение.

## Сборка из исходников

Нужен [Nix](https://nixos.org/download.html) с включёнными flakes.

```bash
# Собрать патч
nix develop --command build-rvp

# Или сразу пропатчить APK
nix develop --command patch-apk target.apk
```

## Как работает

**Runtime-расширение** (`revanced/extensions/`) содержит Java-классы со статическими обёртками. Они
встраиваются в APK как DEX-файл. Каждый метод вызывает реальный Android API, проверяет результат и
возвращает очищенные данные.

Например, `NetworkSandbox.hasTransport(caps, TRANSPORT_VPN)` вызывает настоящий `hasTransport`, но
вернет `false` даже если включен VPN.

**Патч** (`revanced/patches/`) сканирует каждый метод в APK, находит вызовы целевых API и
перенаправляет их на обёртки.

## Предупреждение

Проект создан для изучения того, как приложения собирают данные с устройства. Используйте только на
своих устройствах. Патчинг чужих приложений может нарушать их пользовательское соглашение. Автор не
несёт ответственности за последствия использования. ПО поставляется как есть, без каких-либо
гарантий.

## Лицензия

MIT
