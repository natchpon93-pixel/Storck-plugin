# McInvest — ระบบลงทุนทอง น้ำมัน และหุ้น สำหรับ Minecraft

Plugin สำหรับ **Paper 26.x** ที่ให้ผู้เล่นใช้เงินในเกมซื้อขายทองคำ น้ำมันดิบ และหุ้น
โดยราคาอ้างอิงจากตลาดโลกจริง ผ่านหน้า GUI ที่กดซื้อขายได้ทันที

---

## 1. ต้องมีอะไรบ้าง

| สิ่งที่ต้องมี | หมายเหตุ |
|---|---|
| Paper 26.x | ไม่ใช่ Spigot |
| Vault | ตัวกลางเชื่อมระบบเงิน |
| CMI (หรือ plugin economy อื่น) | ต้องเปิด module Economy |
| Java 25 | สำหรับตอน build เท่านั้น |

**สำคัญ:** เปิดไฟล์ `plugins/CMI/modules.yml` แล้วเช็คว่า `Economy: true`
ถ้าปิดอยู่ Vault จะหาระบบเงินไม่เจอ แล้ว plugin จะปิดตัวเองตอนเปิดเซิร์ฟ

---

## 2. วิธี build ไฟล์ .jar

### วิธีที่ 1 — ให้ GitHub build ให้ (ไม่ต้องลงอะไรในเครื่องเลย)

1. สมัคร GitHub แล้วสร้าง repository ใหม่ (ตั้งเป็น Private ได้)
2. อัปโหลดไฟล์ทั้งหมดในโฟลเดอร์นี้ขึ้นไป (ลากวางในหน้าเว็บได้เลย)
3. รอสักครู่ แล้วเข้าแท็บ **Actions**
4. กดเข้า workflow ที่รันล่าสุด → ตรงส่วน **Artifacts** จะมีไฟล์ `McInvest-jar` ให้โหลด

ทุกครั้งที่แก้โค้ดแล้วอัปโหลดใหม่ มันจะ build ให้อัตโนมัติ

### วิธีที่ 2 — build บน Mac ของตัวเอง

เปิด Terminal แล้วพิมพ์ทีละบรรทัด

```bash
# ลง Homebrew ถ้ายังไม่มี (ข้ามได้ถ้ามีแล้ว)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# ลง Java 25 กับ Maven
brew install openjdk@25 maven

# เข้าไปในโฟลเดอร์โปรเจกต์ แล้ว build
cd ~/Downloads/McInvest
mvn clean package
```

ไฟล์ที่ได้จะอยู่ที่ `target/McInvest-1.0.0.jar`

---

## 3. วิธีติดตั้ง

1. เอาไฟล์ `.jar` ไปวางในโฟลเดอร์ `plugins/` ของเซิร์ฟ
2. รีสตาร์ทเซิร์ฟ
3. plugin จะสร้างไฟล์ `plugins/McInvest/config.yml` ให้อัตโนมัติ
4. พิมพ์ `/invest` ในเกมเพื่อเปิดเมนู

ตอนแรกระบบจะใช้ **โหมด mock** คือราคาปลอมที่วิ่งขึ้นลงสมจริง
ใช้ทดสอบระบบซื้อขายได้ทันทีโดยไม่ต้องมี API key

---

## 4. เปิดใช้ราคาจริง

1. สมัครฟรีที่ https://finnhub.io/register
2. ก๊อป API key มา
3. แก้ `plugins/McInvest/config.yml`

```yaml
market:
  provider: finnhub
  finnhub-token: "ใส่ key ตรงนี้"
```

4. พิมพ์ `/invest reload` ในเกม

**ข้อจำกัดของแพ็กฟรี:** ยิงได้ 60 ครั้ง/นาที ราคาดีเลย์ราว 20 นาที
โค้ดนี้ยิงทีละตัวห่างกัน 1.1 วินาที และดึงใหม่ทุก 5 นาที ตามค่าเริ่มต้นจึงไม่มีทางเกินโควตา

---

## 5. คำสั่งและสิทธิ์

| คำสั่ง | ทำอะไร |
|---|---|
| `/invest` | เปิดหน้าตลาด |
| `/invest portfolio` | เปิดหน้าพอร์ต |
| `/invest price` | ดูราคาทั้งหมดในแชท |
| `/invest update` | สั่งดึงราคาใหม่ทันที (แอดมิน) |
| `/invest reload` | โหลด config ใหม่ (แอดมิน) |

| สิทธิ์ | ค่าเริ่มต้น |
|---|---|
| `mcinvest.use` | ทุกคน |
| `mcinvest.admin` | OP |

---

## 6. ปรับแต่งที่ควรรู้

**`currency-multiplier`** — ตัวคูณแปลงราคา USD เป็นเงินในเกม
ทองคำ (GLD) ราคาราว 300 USD ถ้าตั้งไว้ 10 ในเกมจะเป็น 3,000 ต่อหน่วย
ถ้าผู้เล่นในเซิร์ฟมีเงินเฉลี่ยหลักหมื่น ค่านี้กำลังพอดี ถ้าเงินน้อยกว่านั้นให้ลดลง

**`fee-percent`** — ค่าธรรมเนียมซื้อขาย อย่าตั้งเป็น 0
เพราะจะเปิดช่องให้ผู้เล่นกดซื้อ-ขายรัวๆ ทำกำไรจากราคาที่ขยับนิดเดียว

**เพิ่มหุ้นเอง** — เพิ่มใน `assets:` ของ config

```yaml
assets:
  amd:
    symbol: AMD
    display: "&c&lAMD"
    icon: REDSTONE
    base: 160.0
```

`symbol` ต้องเป็น ticker ที่ Finnhub รู้จัก ส่วน `icon` ต้องเป็นชื่อไอเทมของ Minecraft

---

## 7. ทองกับน้ำมันใช้ ETF เป็นตัวแทน

API ที่ให้ราคาทองหรือน้ำมันโดยตรงส่วนใหญ่โควตาฟรีน้อยมาก
โค้ดนี้จึงใช้กองทุน ETF ที่วิ่งตามราคาสินค้าจริงแทน แล้วยิง API เจ้าเดียวจบ

| ในเกม | ticker | อ้างอิง |
|---|---|---|
| ทองคำ | GLD | SPDR Gold Shares |
| เงิน | SLV | iShares Silver Trust |
| น้ำมันดิบ | USO | United States Oil Fund (WTI) |

ถ้าอยากได้ราคาทองต่อออนซ์แบบเป๊ะๆ ต้องไปใช้ API ทองโดยเฉพาะซึ่งส่วนใหญ่เสียเงิน
สำหรับเซิร์ฟเกม ETF ให้ผลใกล้เคียงจนแยกไม่ออก

---

## 8. โครงสร้างโค้ด

```
src/main/java/com/mcinvest/
├── McInvest.java              จุดเริ่มต้น เชื่อม Vault ตั้ง scheduler
├── market/
│   ├── Asset.java             ข้อมูลสินทรัพย์ 1 ตัว
│   ├── Quote.java             ราคาล่าสุด
│   ├── PriceProvider.java     สัญญาว่าแหล่งราคาต้องทำอะไรได้บ้าง
│   ├── MockPriceProvider.java ราคาปลอมสำหรับเทส
│   ├── FinnhubPriceProvider.java  ราคาจริง
│   └── MarketManager.java     cache ราคา + ตัวจับเวลาดึงราคา
├── portfolio/
│   ├── Holding.java           การถือครอง 1 รายการ
│   ├── PortfolioStore.java    อ่าน/เขียนไฟล์ portfolios.yml
│   └── TradeService.java      ตรรกะซื้อขาย ตัดเงินผ่าน Vault
├── gui/
│   ├── MenuHolder.java        ตัวระบุเมนูและ action ของแต่ละช่อง
│   ├── MarketMenu.java        หน้าตลาด
│   ├── AssetMenu.java         หน้าซื้อขายรายตัว
│   ├── PortfolioMenu.java     หน้าพอร์ต
│   └── GuiListener.java       ดักคลิก
└── command/InvestCommand.java คำสั่ง /invest
```

อยากเปลี่ยนไปเก็บข้อมูลใน SQLite ทีหลัง แก้แค่ `PortfolioStore.java` ไฟล์เดียว
ส่วนอื่นไม่ต้องแตะเลย

---

## 9. ถ้ามีปัญหา

| อาการ | สาเหตุที่พบบ่อย |
|---|---|
| plugin ปิดตัวเองตอนเปิดเซิร์ฟ | CMI ยังไม่ได้เปิด module Economy หรือไม่มี Vault |
| ราคาขึ้นว่า "ยังไม่มีข้อมูล" | รอรอบแรกสักครู่ หรือลอง `/invest update` |
| console เตือน rate limit | เพิ่มค่า `refresh-minutes` ใน config |
| ราคาไม่อัปเดตเลยตอนสุดสัปดาห์ | ปกติ ตลาดหุ้นปิดเสาร์-อาทิตย์ |
| เงินปัดเศษเพี้ยน | ตั้งให้ CMI รองรับทศนิยม |
