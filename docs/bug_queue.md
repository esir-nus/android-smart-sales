| id | title | repro | test_file | status |
| --- | --- | --- | --- | --- |
| BUG-001 | BLE scan never times out | Trigger `BleManager.startScan()` with no `BT311` nearby → `_connectionState` stays `Scanning` forever, drains battery. | device-connectivity/src/test/java/com/smartsales/business/bluetooth/BleBoundedScanTest.kt | red |
| BUG-002 | Wi-Fi connect waits 10s before captive warning | `ConnectivityApiConfig.CONNECT_TIMEOUT_MS` is 10 000 ms so captive portals show failure after 10 s+, not the desired <5 s. | device-connectivity/src/test/java/com/smartsales/data/network/ConnectivityTimeoutTest.kt | red |
| BUG-003 | Room DB wipes configs on upgrade | `SmartSalesDatabase` relies on `fallbackToDestructiveMigration`, so updating the schema deletes Wi-Fi/device history. | device-connectivity/src/test/java/com/smartsales/data/local/database/SmartSalesDatabaseMigrationTest.kt | red |
