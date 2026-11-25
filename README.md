# Android Socket 聊天室

這個專案使用 Java 與 TCP Socket 實作區域網路聊天室。Server 可同時接受兩個以上的 Client 連線，並將聊天訊息及成員加入、離開的通知廣播給聊天室成員。

| 模組 | 角色 | 功能 |
| --- | --- | --- |
| `app` | Server | 輸入名稱後啟動聊天室，顯示 Server IP、Port 與聊天紀錄，可發送訊息或關閉聊天室。 |
| `myapplication` | Client | 輸入名稱、Server IP 和 Port 後連線，可查看聊天室成員的加入與離開通知、收發訊息或離開聊天室。 |

## 建置

用 Android Studio 開啟專案根目錄並同步 Gradle。兩個模組都使用 Android SDK 37 編譯，可在 Android 7.1（API 25）以上的裝置執行。模擬器測試需要安裝 Android Emulator 和 Android SDK Platform-Tools（提供 `adb`）。

也可以在 Windows PowerShell 一次建置兩個模組：

```powershell
.\gradlew.bat :app:assembleDebug :myapplication:assembleDebug
```

## 在同一台電腦上用三台模擬器測試

在 Android Studio 的 **Device Manager** 啟動三台模擬器，然後在 Terminal 執行 `adb devices` 確認序號。以下是本專案的操作範例，請依實際序號調整。

| 模擬器 | 執行模組 | 名稱範例 |
| --- | --- | --- |
| `emulator-5556` | `app` | Server |
| `emulator-5554` | `myapplication` | Client1 |
| `emulator-5558` | `myapplication` | Client2 |

1. 在 `emulator-5556` 執行 `app`，輸入 Server 名稱，按「啟動 Server（Port 8888）」。進入第二頁後會顯示 Server 已加入聊天室、IP 與 Port。
2. 在 Android Studio 的 Terminal 設定並確認轉送：

   ```powershell
   adb -s emulator-5556 forward tcp:8888 tcp:8888
   adb forward --list
   ```

   清單應包含 `emulator-5556 tcp:8888 tcp:8888`。
3. 在另外兩台模擬器分別執行 `myapplication`。輸入不同的 Client 名稱，兩台的 **Server IP 都填 `10.0.2.2`**，**Port 都填 `8888`**，再依序按「連線」。`10.0.2.2` 會連到電腦的本機位址，再經由 ADB 轉送至 Server 模擬器。詳見 [Android Emulator 網路位址文件](https://developer.android.com/studio/run/emulator-networking-address)。

## 驗收流程

1. 先連上 Client1，再連上 Client2。Server 和 Client1 應看到 Client2 的加入通知，Client2 應看到 Client1 已在聊天室。
2. 從兩個 Client 各送一則訊息，再由 Server 送一則回覆，確認三個畫面都能看到訊息。
3. 在 Client2 按「離開聊天室」。Client2 應回到連線頁，Server 和 Client1 應顯示 Client2 的離開通知，且 Client1 仍能繼續聊天。
4. 在 Server 按「關閉聊天室」。仍連線的 Client 應收到斷線提示並回到連線頁。

## 使用實體裝置

讓 Server 和 Client 裝置連到同一個可互相通訊的 Wi-Fi。Client 填入 Server 第二頁顯示的區域網路 IP 和 Port `8888`，實體裝置間連線可直接使用區域網路。

如果連不上，先確認 Server 仍停留在聊天室頁面，再核對 IP 和 Port。使用模擬器時，也可以用 `adb forward --list` 確認轉送是否仍指向正確的模擬器。
