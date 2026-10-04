# Spark ERP – Kullanım Kılavuzu

**Sürüm 2.9.0** · Elektrik ustaları ve taahhüt firmaları için iş takip programı

Spark ERP; şantiyelerinizi, servis işlerinizi, günlük iş programınızı, tekliflerinizi, kullandığınız malzemeleri, personel puantajını ve tahsilatlarınızı tek bir yerde tutar. Hangi işte ne kadar kazandığınızı, kimden ne kadar alacağınız kaldığını ve ay sonunda ne kazandığınızı birkaç tıkla görürsünüz.

İnternet bağlantısı gerekmez. Bütün veriler kendi bilgisayarınızda saklanır.

---

## İçindekiler

1. Neler yapabilirsiniz?
2. Kurulum ve ilk açılış
3. Önerilen ilk adımlar
4. Ekran düzeni
5. Ana Ekran
6. Şantiyeler
7. Servisler
8. İş Takip (günlük program)
9. Teklifler
10. Malzeme girişi
11. KDV nasıl çalışır?
12. Puantaj (personel çalışma günleri)
13. Tahsilatlar
14. Aylık Rapor
15. Müşteriler, Ürünler ve Personel
16. Kullanıcılar ve yetkiler
17. Ayarlar
18. Silme, toplu silme ve pasife alma
19. Tutar girerken bilmeniz gerekenler
20. Hesaplar nasıl yapılır?
21. Sık sorulan sorular

---

## 1. Neler yapabilirsiniz?

| Konu | Neler yapılır |
|---|---|
| **Şantiyeler** | Şantiye açma, malzeme ve puantaj girme, tahsilat kaydetme, işi bitirme, PDF döküm alma |
| **Servisler** | Tek formda servis kaydı (ücret, işçilik, değişen malzemeler), kısmi ya da tam ödeme takibi |
| **İş Takip** | Günlük ve haftalık iş programı, ekip ataması, "gidildi / gidilmedi" takibi, günlük program PDF'i |
| **Teklifler** | Firma logolu teklif formu, iskonto ve işçilik, hazır şart metinleri, teklifi tek tıkla işe dönüştürme |
| **Malzeme** | Ürün kataloğu, son fiyatların önerilmesi, tedarikçi karşılaştırması, dolar/euro ile alış |
| **Puantaj** | Personelin hangi gün hangi işte çalıştığı, tam gün / yarım gün, yevmiye kaydı |
| **Tahsilat** | Nakit, havale, çek ve kart ile alınan ödemeler; kalan alacak |
| **Raporlar** | Aylık ve yıllık ciro, maliyet ve kâr; Excel ve PDF çıktısı |
| **Kullanıcılar** | Şifreyle giriş; mali bilgileri göremeyen "Yönetici" kullanıcıları |
| **Güvenlik** | Program kapanırken otomatik yedek, istenen an elle yedek, yedekten geri yükleme |

---

## 2. Kurulum ve ilk açılış

### Sistem gereksinimi
- Windows 10 veya Windows 11
- Java ya da başka bir program kurmanız gerekmez; gereken her şey kurulumun içindedir.

### Kurulum
1. Size gönderilen **`Spark ERP-2.9.0.exe`** dosyasına çift tıklayın.
2. Mavi bir **"Windows bilgisayarınızı korudu"** penceresi çıkarsa **Ek bilgi**'ye, ardından **Yine de çalıştır**'a basın. Bu uyarı, program imzalı bir yazılım şirketinden gelmediği için çıkar; programa zarar vermez.
3. "Bu uygulamanın değişiklik yapmasına izin verilsin mi?" sorusuna **Evet** deyin.
4. Lisans sözleşmesini kabul edin, kurulum klasörünü olduğu gibi bırakıp **Yükle**'ye basın.
5. Kurulum bitince masaüstünde ve Başlat menüsünde **Spark ERP** kısayolu oluşur.

### İlk açılış
1. Kısayola tıkladığınızda önce **"Spark ERP – Yükleniyor"** resmi görünür. Program birkaç saniye ile yarım dakika arasında açılır; **bu sırada tekrar tıklamayın.** Tekrar tıklarsanız "Spark ERP zaten açık" uyarısı çıkar, bu normaldir.
2. Lisans sözleşmesini okuyup **"Lisans sözleşmesini okudum, kabul ediyorum"** kutusunu işaretleyin ve **Devam**'a basın. Bu ekran her sürümde yalnızca bir kez çıkar.
3. **İlk Kurulum** ekranında kendi hesabınızı oluşturun: kullanıcı adı, ad soyad, unvan ve en az 6 karakterli şifre. Bu hesap her şeyi görebilen **Admin** hesabıdır.

> ⚠️ **Şifrenizi bir yere yazın.** Şifreler geri okunamayacak şekilde saklanır. Tek admin şifresini unutursanız programa kendiniz giremezsiniz.

### Eski "Şantiye Takip" sürümünden geçiş
Daha önce **Şantiye Takip** kullanıyorduysanız:
1. Eski programı kapatın.
2. Güvenlik için yedek alın: Dosya Gezgini'nin adres çubuğuna `%APPDATA%` yazın, **SantiyeTakip** klasörünü bir USB belleğe kopyalayın.
3. Spark ERP'yi kurun. Eski program otomatik kaldırılır.
4. İlk açılışta **"Verileriniz yeni sürüme aktarıldı"** mesajı çıkar. Bütün müşteri, iş, malzeme, puantaj ve tahsilat kayıtlarınız yeni programdadır. Eski klasör yedek olarak yerinde bırakılır.
5. Eski sürümde kullanıcı girişi yoksa **İlk Kurulum** ekranı bir kez çıkar; hesabınızı oluşturun. Verileriniz bundan etkilenmez.

### Güncelleme
Yeni sürümün kurulum dosyasını çalıştırmanız yeterlidir. Eski sürümün üzerine kurulur ve **verileriniz korunur**.

---

## 3. Önerilen ilk adımlar

Programı ilk kez kullanıyorsanız şu sırayla ilerlemeniz en kolayıdır:

1. **Ayarlar → Firma Bilgileri:** firma adı, adres, telefon, vergi bilgileri ve logonuzu girin. Bunlar teklif ve döküm PDF'lerinin başlığında çıkar.
2. **Ayarlar → Yedekleme:** yedeklerin kaydedileceği klasörü seçin. Harici disk ya da OneDrive / Google Drive klasörü önerilir.
3. **Personel:** ustalarınızı ve işçilerinizi günlük yevmiyeleriyle ekleyin.
4. **Kullanıcılar:** programı başkası da kullanacaksa ona ayrı bir kullanıcı açın (bkz. Bölüm 16).
5. Dövizle malzeme alıyorsanız **Ayarlar → Güncel Kurlar**'a dolar ve euro kurunu girin.
6. **Şantiyeler → Yeni Şantiye** ile ilk işinizi açın.

Ürünleri ve müşterileri önceden tek tek girmeniz gerekmez: malzeme veya iş eklerken yeni ürünü ve yeni müşteriyi aynı pencereden oluşturabilirsiniz.

---

## 4. Ekran düzeni

- **Sol menü:** Ana Ekran, Şantiyeler, Servisler, İş Takip, Teklifler, Müşteriler, Ürünler, Personel, Personel Puantajı, Aylık Rapor, Kullanıcılar, Ayarlar ve Hakkında. İş Takip'in yanındaki sayı, kapatılmamış eski işlerin sayısıdır.
- **Sağ üst köşe:** giriş yapan kullanıcının adı. Tıklayınca **Şifremi Değiştir**, **Kullanıcı Değiştir** (kısayol **Ctrl+Shift+L**) ve **Çıkış** seçenekleri gelir.
- **Tema:** açık / koyu görünüm düğmesi üst çubuktadır. Ayarlar'dan "Sistem" seçerseniz Windows'un temasına uyar.
- Bir formda kaydetmediğiniz değişiklik varken başka ekrana geçerseniz program sizi uyarır.

---

## 5. Ana Ekran

Programın açılış ekranıdır ve günün özetini verir.

### Özet kartları
| Kart | Ne gösterir |
|---|---|
| **Aktif Şantiye** | Devam eden şantiye sayısı |
| **Bu Ayın Cirosu** | Bu ay girilen malzeme satışları ve ücretler (KDV hariç) |
| **Bu Ayın Kârı** | Bu ayın cirosu − bu ay girilen malzemelerin alış maliyeti (KDV hariç) |
| **Şantiye Alacağı** | Şantiyelerden toplam alınacak para |
| **Servis Alacağı** | Ödemesi tamamlanmamış servislerden alınacak para |

Kârın altında **"Tahmini"** yazıyorsa bazı malzemelerin alış fiyatı girilmemiştir. Bu kalemlerin maliyeti kâra yansımadığı için gösterilen kâr gerçekte olduğundan yüksek olabilir.

> Ana Ekran'daki ay rakamları malzemeyi **girildiği tarihin ayına** yazar. Aylık Rapor ise işi **başladığı aya** yazar (bkz. Bölüm 14). Bu yüzden ay ortasında başlayan bir şantiyede iki ekranın rakamı farklı olabilir; ikisi de doğrudur.

### Diğer bölümler
- **Bitmiş ama tahsilatı eksik şantiyeler:** "Bitti" yapılmış ama hâlâ parası alınmamış şantiyeler. Satıra tıklayınca şantiye açılır.
- **Bekleyen işler:** İş Takip'te geçmiş günlerden kalmış, kapatılmamış işler. Tıklayınca İş Takip açılır.
- **Son hareketler:** en son açılan işler, girilen malzemeler ve tahsilatlar.

---

## 6. Şantiyeler

Sol menüden **Şantiyeler**'i açın.

### Yeni şantiye açma
**Yeni Şantiye**'ye basın:

| Alan | Açıklama |
|---|---|
| Müşteri | Listeden seçin ya da **Yeni Müşteri** ile ekleyin |
| Şantiye Adı | Örnek: *Ümraniye Blok B elektrik tesisatı* |
| Şantiye Adresi | İsteğe bağlı |
| Başlangıç / Bitiş Tarihi | Bitiş tarihi boş bırakılabilir |
| Durum | **Aktif** veya **Bitti** |
| Servis Ücreti / İşçilik Ücreti | Varsa sabit ücretler ve KDV seçimleri |

### Şantiye kutusu
Her şantiyenin başlığında **müşteri – şantiye adı**, durumu ve sağda **kalan alacak** yazar. Başlığa tıklayınca kutu açılır:

- **İşlem düğmeleri:** Malzeme Ekle · Tahsilat Ekle · Puantaj Gir · Gün Planla · Düzenle · PDF Al · Sil
- **Müşteri bilgisi:** telefon ve adres, müşteriyi hemen arayabilmeniz için üstte yazar.
- **Özet kutuları:**
  - **Toplam (KDV dahil):** altında KDV'siz tutar ve KDV ayrı ayrı yazar
  - **Tahsil Edilen**
  - **Kalan:** kırmızıysa alacak var, yeşilse ödeme tamam
  - **Kâr (KDV hariç)**
- **Sekmeler:** Malzemeler, Puantaj, Tahsilatlar

Üstteki arama kutusuna müşteri ya da şantiye adı yazarak arama yapabilirsiniz. **Sadece aktif** kutusunun işaretini kaldırırsanız bitmiş şantiyeler de listelenir.

**Gün Planla**, bu şantiye için İş Takip'e bir iş ekler (bkz. Bölüm 8).

### Şantiyeyi bitirme
**Düzenle** ile açıp **Durum**'u **Bitti** yapın. Şantiyede kalan alacak varsa program sizi uyarır. Yine de devam ederseniz şantiye, Ana Ekran'daki "tahsilatı eksik" listesine düşer.

### PDF döküm
**PDF Al**, müşteriye verebileceğiniz bir döküm hazırlar: firma başlığınız, müşteri bilgileri, malzeme listesi, ücretler, KDV'li toplamlar ve tahsilatlar. **Alış fiyatlarınız, tedarikçileriniz, kârınız ve yevmiyeler bu belgede yer almaz.**

---

## 7. Servisler

Kısa süreli arıza ve tamir işleri içindir. Sol menüden **Servisler**'i açın.

### Yeni servis
**Yeni Servis**'e basın. Tek bir formda şunları girersiniz:
- Müşteri, tarih, açıklama (örn. *Pano arızası*)
- Servis ücreti ve işçilik ücreti (KDV seçimleriyle)
- **Malzeme Ekle** ile değişen parçalar

Formun altında **ara toplam / KDV / genel toplam** siz yazdıkça hesaplanır.

### Ödeme
- Paranın tamamını aldıysanız **"Ödemenin tamamı alındı"** kutusunu işaretleyin.
- Parça parça ödeme aldıysanız formdaki **Tahsilatlar** bölümünden tek tek ekleyin. Kalan tutar otomatik hesaplanır.

### Servis listesi
- Üstteki **Tümü / Alınmadı / Alındı** düğmeleriyle filtreleyebilirsiniz.
- Ödemesi tamamlananlar yeşil, bekleyenler kırmızı işaretle görünür.
- **Çift tıklama** servisi düzenler. **Sağ tık** menüsünde Düzenle, Tahsilat Ekle, Puantaj Gir, PDF Al ve Sil bulunur.

---

## 8. İş Takip (günlük program)

Ekibinizin hangi gün nereye gideceğini planladığınız ekrandır. Sol menüden **İş Takip**'i açın.

### Sekmeler
- **Günlük:** seçili günün işleri kartlar halinde. Üstte planlanan, tamamlanan ve gidilmeyen iş sayıları yazar. **Bugün** düğmesi bugüne döner.
- **Haftalık:** haftanın günleri ve her gündeki işler. **Bu hafta** düğmesi bu haftaya döner.
- **Bekleyen İşler:** tarihi geçtiği halde kapatılmamış işler ve gidilemeyen işler.

### İş ekleme
**İş Ekle**'ye basın:

| Alan | Açıklama |
|---|---|
| Tarih / Saat | Saat isteğe bağlıdır (örn. 09:00) |
| Acil | İşaretlenirse kartta kırmızı **ACİL** etiketi çıkar |
| Başlık | Örn. *Ahmet Bey – priz arızası*. Hazır iş tanımı şablonlarından seçebilirsiniz |
| Müşteri, adres, telefon | Müşteri listeden seçilebilir ya da yazılabilir |
| Giden personel | O işe gidecek kişiler. Aynı gün başka işe yazılı biri seçilirse program uyarır |
| Bir işe bağla | İsteğe bağlı olarak var olan bir servise bağlar |
| Not | İsteğe bağlı |

### İşi kapatma
Kartın üzerindeki düğmelerle:
- **Gidildi:** iş tamamlanır. İsterseniz kısa bir not yazın. **"Puantaja da yaz"** işaretliyse giden personele o gün için puantaj kaydı düşülür. İş bir servise bağlı değilse **Servisler** listesinde yeni bir servis kendiliğinden açılır; ücret ve malzemeyi oradan girersiniz.
- **Gidilmedi:** sebebini yazın. **"Ertesi güne aktar"** işaretliyse iş ertesi güne taşınır.
- **İptal et:** iş iptal edilir.
- **Bugüne aktar:** geçmiş bir işi bugünün programına alır.

### Hatırlatma
Program açılırken geçmiş günlerden kapatılmamış iş varsa **Bekleyen işler** penceresi çıkar. Her işi **Bugüne Al**, **Tamamlandı** ya da **İptal** ile kapatabilir, hepsini **Tümünü Bugüne Al** ile taşıyabilir veya **Sonra Bakarım** diyebilirsiniz. Bu hatırlatmayı **Ayarlar → Görünüm ve hatırlatmalar**'dan kapatabilirsiniz.

### Günlük program PDF'i
**Günlük Program PDF**, seçili günün işlerini saat, adres, telefon ve giden personelle tek sayfada verir. Yazdırıp ekibe verebilirsiniz.

---

## 9. Teklifler

Müşteriye fiyat vermek için kullanılır. Sol menüden **Teklifler**'i açın.

### Teklif listesi
- Üstteki düğmelerle durumlara göre filtreleyin: **Tümü / Taslak / Gönderildi / Kabul / Red**.
- Geçerlilik süresi dolmuş ve hâlâ yanıt bekleyen teklifler sarı **"Süresi doldu"** etiketiyle görünür.
- **Aç** (veya çift tıklama), **Kopyala**, **PDF Al** ve **Sil** düğmeleri seçili teklife uygulanır. **Kopyala**, benzer bir teklifi baştan yazmadan hazırlamanızı sağlar.

### Yeni teklif
**Yeni Teklif**'e basın:

1. **Müşteri bilgileri:** Firma, adres, ilgili kişi, telefon, faks, e-posta, konu. Firma adını yazarken kayıtlı müşteriler önerilir. Kayıtlı olmayan bir müşteriyi **"Bu müşteriyi müşteri listesine de ekle"** ile listeye ekleyebilirsiniz.
2. **Teklif bilgileri:** tarih, teklif no (otomatik verilir; **Yeni No** ile yenisini alabilirsiniz), geçerlilik (gün), durum.
3. **Kalemler:** **Ürün Ekle** ile satır ekleyin.
   - **Kayıtlı ürün:** katalogdan seçilir, son satış fiyatı öneri olarak gelir.
   - **Serbest kalem:** katalogda olmayan bir şey (örn. sarf malzeme) için ad yazarsınız.
   - Satırları **Yukarı / Aşağı** ile sıralayabilir, **Araya Ekle** ile araya satır ekleyebilir, **Satırı Sil** ile silebilirsiniz.
   - Bir teklifte en fazla **25 kalem** olur. Böylece PDF tek sayfaya sığar; 20 kalemden sonra yazı küçülür.
   - **Hazır listeden ekle** ile daha önce kaydettiğiniz bir malzeme listesini tek seferde ekleyebilirsiniz.
4. **Toplamlar:** iskonto (**Yok / Tutar / Yüzde**), işçilik ve KDV oranını seçin. Teklif fiyatları **KDV hariçtir**; KDV en sonda eklenir.
5. **Notlar / Şartlar:** ödeme şekli, geçerlilik gibi şartlar. **Hazır şart metni seç** ile kayıtlı şartlarınızı getirebilir, yazdığınızı **Şablon olarak kaydet** ile saklayabilirsiniz.
6. **Hazırlayan:** boş bırakırsanız giriş yapan kullanıcının adı ve unvanı yazılır.

**Kaydet** ve ardından **PDF Al** ile firma logolu teklif formunu alırsınız.

### Teklifi işe dönüştürme
Müşteri teklifi kabul ettiyse teklifteki **İşe Dönüştür**'e basın ve **şantiye** mi **servis** mi olacağını seçin. Kalemler, satış fiyatlarıyla birlikte işe malzeme olarak aktarılır. Alış fiyatını ve tedarikçiyi malzemeyi gerçekten aldıktan sonra girersiniz. Dönüştürülmüş teklifte **"Bu teklif işe dönüştürüldü – işi aç"** bağlantısı görünür.

---

## 10. Malzeme girişi

**Malzeme Ekle** penceresi şantiye ve servislerde aynıdır.

| Alan | Açıklama |
|---|---|
| **Tarih** | Malzemenin verildiği gün |
| **Ürün** | Yazmaya başlayın, katalogdan eşleşenler listelenir. Katalogda yoksa **"+ '…' ürününü ekle"** ile oradan ekleyin |
| **Miktar** | Örn. 200 (metre), 2,5 (kg) |
| **Satış Fiyatı** | **Birim fiyat** *veya* **Toplam tutar** girin; diğeri kendiliğinden hesaplanır |
| **Satış KDV** | Bkz. Bölüm 11 |
| **Alış Fiyatı** | İsteğe bağlı ama **kârın doğru hesaplanması için önerilir**; kendi KDV seçimi (**Alış KDV**) vardır |
| **Para birimi ve kur** | Alış TL dışında yapıldıysa **USD** veya **EUR** seçin ve o günün kurunu girin (1 birim = ? ₺). TL karşılığı alış günündeki kurla bir kez hesaplanır, sonradan kur değişse de değişmez |
| **Tedarikçi** | Daha önce yazdığınız tedarikçiler önerilir. Yeni bir ad da yazabilirsiniz |
| **Not** | İsteğe bağlı |

**Akıllı öneriler:** Bir ürünü seçtiğinizde o ürünün son alış fiyatı, satış fiyatı, tedarikçisi ve KDV seçimi otomatik doldurulur. Değiştirmek isterseniz üzerine yazmanız yeterlidir.

**Kaydet ve yeni ekle** ile pencereyi kapatmadan art arda malzeme girebilirsiniz.

Girilmiş bir malzemeyi düzeltmek için **Malzemeler** sekmesinde satıra çift tıklayın ya da seçip **Düzenle**'ye basın.

> "Tedarikçi" alanında büyük/küçük harf ve boşluk farkı önemsizdir: *"Elektrik Market"* ile *"elektrik  market"* aynı tedarikçi sayılır.

---

## 11. KDV nasıl çalışır?

Her fiyatın yanında tek bir açılır liste vardır:

- **KDV yok**
- **%20 KDV hariç** / **%20 KDV dahil**
- **%10 KDV hariç** / **%10 KDV dahil**
- **%1 KDV hariç** / **%1 KDV dahil**

Seçim yaptığınız anda listenin yanında tutarın karşılığı görünür:

| Girilen tutar | Seçim | Yanında yazan |
|---|---|---|
| 1.000,00 | %20 KDV **hariç** | KDV'li: 1.200,00 ₺ |
| 1.200,00 | %20 KDV **dahil** | KDV'siz: 1.000,00 ₺ |

- Malzemelerde **satış** ve **alış** KDV'si ayrı ayrı seçilir.
- Şantiye ve servis özetinde toplamın altında **KDV'siz tutar + KDV** ayrıca yazar. PDF dökümde her KDV oranı matrahıyla ayrı satırda gösterilir.
- **Kâr her zaman KDV hariç tutarlar üzerinden hesaplanır.** Kalan alacak ise müşterinin ödeyeceği KDV dahil tutardır.
- Tekliflerde fiyatlar KDV hariç girilir, KDV oranı teklifin altında bir kez seçilir.

---

## 12. Puantaj (personel çalışma günleri)

Hangi personelin hangi gün hangi işte çalıştığını kaydeder.

> **Önemli:** Puantaj yalnızca **kayıt amaçlıdır**. Yevmiyeler şantiye kârından ve aylık kârdan **düşülmez**. Raporlarda bilgi olarak ayrı gösterilir.

### Puantaj girme
Şantiye kutusunda **Puantaj Gir**'e basın. Servisler için bu seçenek sağ tık menüsündedir. İş Takip'te **"Puantaja da yaz"** ile de otomatik kayıt düşülebilir.

1. **Tek gün** veya **Tarih aralığı** seçin. Aralıkta istenirse **Cumartesi atla / Pazar atla** işaretlenir.
2. Çalışan personeli işaretleyin. Yevmiye, personelin kayıtlı yevmiyesiyle gelir ve değiştirilebilir.
3. **Katsayı:** **1,0** tam gün, **0,5** yarım gün.
4. Pencerenin altında özet görünür, örn. *3 tarih × 2 kişi = 6 gün*. **Kaydet**'e basın.

Program sizi şu durumlarda uyarır:
- Personel aynı gün **başka bir işte** de kayıtlıysa
- Bir personelin aynı gündeki toplamı **1 günü aşıyorsa**
- Tarih, işin başlangıç–bitiş aralığının dışındaysa

Aynı kişiyi aynı işe aynı gün ikinci kez eklerseniz tekrar kayıt oluşmaz.

### Puantaj sekmesi
Önce personel bazında özet görünür: gün sayısı, ortalama yevmiye, toplam ve çalıştığı günler. Altında **günlük kayıtlar** yer alır. Yevmiye, katsayı veya açıklamayı düzeltmek için hücreye çift tıklayın. Bir satırı silmek için seçip **Sil**'e basın.

### Personel Puantajı ekranı
Sol menüden **Personel Puantajı:** bir personel ve tarih aralığı seçin.
- **İş Bazında:** hangi şantiye veya serviste kaç gün çalıştığı ve toplam
- **Takvim:** gün gün tablo; **Excel'e Aktar** ile dosyaya alınabilir. Yarım günler "(½)" ile işaretlenir.

---

## 13. Tahsilatlar

**Tahsilat Ekle** ile müşteriden alınan her ödemeyi girin: tarih, tutar, yöntem (**Nakit / Havale / Çek / Kart**) ve isteğe bağlı not.

- **Tahsilatlar** sekmesinde tüm ödemeler listelenir. Çift tıklama düzenler, **Sil** kaldırır.
- **Kalan = KDV dahil toplam − tahsil edilenler.**
- Müşteri toplamdan fazla ödediyse işin başlığında **"Tahsilat tamam"** yazar.

---

## 14. Aylık Rapor

"Bu ay ne kazandım?" sorusunun cevabıdır. Sol menüden **Aylık Rapor**'u açın.

### Tarih kuralı
Her iş, **kendi tarihinin ayına** yazılır: şantiyede başlangıç tarihi, serviste servis tarihi. Eylülde yaptığınız bir işin parasını ekimde alsanız da o iş **Eylül** cirosunda görünür. Henüz alınmayan para **Tahsil Edilmeyen** olarak ayrıca gösterilir.

### Aylık sekme
- **Ay seçimi:** ◀ ▶ okları, ay ve yıl kutuları, **Bu ay** düğmesi
- **Filtre:** Tümü / Sadece şantiyeler / Sadece servisler
- **Kartlar:**
  - **Ciro (KDV hariç):** altında KDV tutarı bilgi olarak yazar
  - **Maliyet:** malzeme alışları; altında o ayın yevmiye toplamı bilgi olarak yazar
  - **Kâr:** ciro − maliyet; alış fiyatı eksikse "Tahmini" olarak işaretlenir
  - **Tahsil Edilmeyen**
- **İş tablosu:** Tarih, Müşteri, İş, Malzeme, Yevmiye, Ciro, Maliyet, Kâr, Tahsilat. En altta **TOPLAM** satırı vardır. Bir satıra çift tıklayınca o iş açılır.
- **Malzeme özeti:** o ay en çok kullanılan ürünler, toplam miktar, alış ve satış
- **Yevmiye özeti:** personel bazında gün sayısı ve yevmiye toplamı
- **Excel'e Aktar** ve **PDF Al** ile raporu dosyaya kaydedebilirsiniz. Excel'de tutarlar sayı olarak gelir, toplama yapabilirsiniz.

### Yıllık sekme
Seçtiğiniz yılın 12 ayı tek tabloda görünür: Ay, Ciro, Maliyet, Kâr, Tahsil Edilmeyen ve yıl toplamı. Altında **aylık kâr grafiği** bulunur.

> Aylık Rapor yalnızca **Admin** kullanıcılara açıktır.

---

## 15. Müşteriler, Ürünler ve Personel

### Müşteriler
Ad Soyad / Firma, telefon, e-posta, adres, vergi no ve not. Telefon ve adres şantiye kutusunda, adres ve vergi no PDF dökümde görünür. İşi olan bir müşteri silinemez; pasife alınabilir.

### Ürünler
Katalogdaki ürünler: ad, birim (Adet, Metre, Kg, Takım, Paket), **marka**, **kategori**, varsayılan tedarikçi ve alış maliyeti.
- Üstteki arama kutusuna ürün adı veya marka yazın. Soldaki **Kategoriler** listesi ve marka / tedarikçi kutularıyla filtreleyin. **Filtreleri temizle** hepsini sıfırlar.
- Bir ürün seçince sağda **Ürün Kullanım Geçmişi** açılır:
  - **Kullanım Geçmişi** sekmesi: ürünün kime, ne zaman, kaç tane ve kaça verildiği; en düşük, en yüksek ve son satış fiyatı.
  - **Tedarikçi Karşılaştırması** sekmesi: her tedarikçiden ortalama, en düşük ve en yüksek alış fiyatı. En ucuz tedarikçi en üstte.
  - Tarih filtresi ve **Excel'e Aktar**. Bir satıra çift tıklarsanız ilgili iş açılır.

### Personel
Ad, günlük yevmiye, **Usta** işareti ve **Aktif** durumu. Puantaj kaydı olan personel silinemez; program bunun yerine **pasife almayı** önerir. Pasif personel puantaj ve iş takip ekranlarında listelenmez.

---

## 16. Kullanıcılar ve yetkiler

Sol menüden **Kullanıcılar** (yalnızca Admin görür).

| Rol | Neleri görür |
|---|---|
| **Admin** | Her şeyi |
| **Yönetici** | **Ana Ekran, Şantiyeler, Aylık Rapor ve Kullanıcılar dışındaki** bütün ekranlar. Bu dört ekran menüde kilitli ve gri görünür; giriş yapınca Servisler açılır. Kâr, maliyet, alış fiyatı, tedarikçi, alacak, tahsilat ve yevmiye bilgilerini göremez; silme yapamaz |

- **Yeni Kullanıcı:** kullanıcı adı, ad soyad, unvan, rol ve şifre. Unvan, teklif PDF'indeki "Hazırlayan" bölümünde çıkar.
- **Şifresini Değiştir:** bir kullanıcının şifresini unuttuysa Admin buradan yeni şifre verir.
- **Pasif/Aktif Yap:** işten ayrılan bir kullanıcının girişini kapatır; geçmiş kayıtları silinmez.
- Herkes kendi şifresini sağ üstteki menüden **Şifremi Değiştir** ile değiştirebilir.
- Üst üste 5 yanlış şifre girilirse giriş 30 saniye bekletilir.

---

## 17. Ayarlar

### Görünüm ve hatırlatmalar
- **Tema:** Açık, Koyu veya Sistem (Windows'a uyar). Her kullanıcının seçimi ayrı saklanır.
- **Açılışta bekleyen işleri hatırlat:** İş Takip hatırlatma penceresini açar/kapatır.

### Firma Bilgileri
Firma adı, slogan, adres, telefon, e-posta, web, vergi dairesi, vergi no, **logo** ve **kurumsal renk**. Bunlar teklif, şantiye/servis dökümü ve günlük program PDF'lerinin başlığında kullanılır.
- Logoyu kutunun içine sürükleyin veya tıklayıp seçin (PNG, JPG, SVG). Önerilen: 400×200 piksel, şeffaf arka planlı PNG.
- Sağdaki önizleme, logonun PDF'te nasıl görüneceğini gösterir.
- **Firma Bilgilerini Kaydet**'e basmayı unutmayın.

### Güncel Kurlar
Dolar ve euro kurunu elle girin (program internete bağlanmaz). Bu kur yeni dövizli alışlarda öneri olarak gelir; kaydedilmiş kalemlerin kuru değişmez.

### Şablonlar
Sık yazdığınız metinleri ve listeleri saklar. Üç türü vardır:

| Tür | Ne işe yarar |
|---|---|
| **Teklif Şartları** | Teklifin en altındaki şartlar/notlar bölümü. **Varsayılan** yapılan şablon her yeni teklife otomatik gelir |
| **Hazır Malzeme Listesi** | Teklife tek seferde eklenen kalem listesi (örn. "Daire elektrik paketi") |
| **Hazır İş Tanımı** | İş Takip'te iş eklerken başlık olarak seçilir |

Soldaki şema, seçili şablonun teklif PDF'inde nereye geldiğini gösterir. Her şablonun ne zaman ve kaç kez kullanıldığı listede yazar.

### Yedekleme
- **Otomatik yedek:** program her kapandığında verilerinizin bir yedeğini alır. En son **30 yedek** saklanır, daha eskileri silinir.
- **Yedek Klasörü:** varsayılan olarak `%APPDATA%\SparkERP\yedekler` klasörüdür. **Klasör Seç** ile değiştirebilirsiniz.
- **Şimdi Yedek Al:** o anda elle yedek alır.
- **Yedekten Geri Yükle:** seçtiğiniz yedeği geri yükler. Mevcut veriler yedektekilerle değişir ve program kapanır; sonra yeniden açmanız gerekir.

> 💡 Bilgisayar arızasına karşı yedek klasörünü **harici disk** ya da **bulut klasörü** (OneDrive, Google Drive) yapmanızı öneririz.

### Excel Dışa Aktarım
Başlangıç ve bitiş tarihi seçip **Excel'e Aktar**'a basın. O tarihlerdeki tüm işler; satış, KDV, maliyet, kâr ve tahsilat bilgileriyle bir tabloya, malzemeler de ayrı bir sayfaya aktarılır.

### Verilerin yeri
Tüm veriler tek bir dosyada tutulur: `%APPDATA%\SparkERP\veri.db`. Bu dosyayı elle silmeyin veya taşımayın; yedek ve geri yükleme için **Ayarlar** ekranını kullanın.

---

## 18. Silme, toplu silme ve pasife alma

- Her silme işleminden önce program, silinecek kaydı ve ona bağlı kayıtları (malzeme, puantaj, tahsilat sayıları) gösterip onay ister. **Silme geri alınamaz.**
- **Toplu silme:** listelerde satırların başındaki kutuları işaretleyin (**Görünen tüm satırları seç** ile hepsini seçebilirsiniz) ve **Seçilenleri Sil**'e basın. Çok sayıda kayıt silinirken onay için kayıt sayısını yazmanız istenir.
- İşlem kaydı olan müşteri, ürün ve personel silinemez. Program bunları **pasife almayı** önerir: pasif kayıtlar listelerde kalır, yeni kayıt girerken seçim listelerinde görünmez.

---

## 19. Tutar girerken bilmeniz gerekenler

Bütün tutar ve miktar kutuları aynı şekilde çalışır:

- Sadece **rakam** ve **ondalık için virgül** yazılır. Harf yazılamaz.
- **Binlik noktalarını siz koymazsınız**, program yazarken kendisi ekler: `1000` yazınca `1.000` olur.
- Kutudan çıkınca tutar **`1.000,00`** biçimine getirilir.
- Ondalık için **virgül** kullanın: 12,5 için `12,5` yazın.
- Kopyalayıp yapıştırdığınız `1.250,00 ₺` veya `1000.50` gibi tutarlar doğru okunur.
- Tarihler **gg.aa.yyyy** biçimindedir (örn. 25.09.2026).

---

## 20. Hesaplar nasıl yapılır?

| Terim | Hesap |
|---|---|
| **Satış / Ciro** | Malzeme satışları + servis ücreti + işçilik ücreti, **KDV hariç** |
| **Genel toplam** | Satış + KDV (müşterinin ödeyeceği tutar) |
| **Maliyet** | Malzemelerin **alış** fiyatları, KDV hariç (dövizli alışlar alış günündeki kurla TL'ye çevrilir) |
| **Kâr** | Ciro − Maliyet |
| **Kalan** | Genel toplam − Tahsil edilenler |
| **Yevmiye** | Günlük ücret × katsayı (tam gün 1, yarım gün 0,5); **kâra dahil edilmez** |
| **"Tahmini" kâr** | Bazı malzemelerin alış fiyatı girilmemiş; o kalemlerin maliyeti eksik |
| **Teklif** | Kalemler − iskonto + işçilik = ara toplam; ara toplam + KDV = genel toplam |

KDV her oran için toplam tutar üzerinden bir kez hesaplanır ve kuruşa yuvarlanır; bu yüzden satır satır yuvarlama farkı oluşmaz.

---

## 21. Sık sorulan sorular

**Program açılmıyor, tıklayınca bir şey olmuyor.**
Açılış birkaç saniye ile yarım dakika sürebilir; "Yükleniyor" resmini bekleyin. "Spark ERP zaten açık" uyarısı çıkıyorsa program zaten çalışıyordur, görev çubuğundaki pencereye tıklayın.

**Şifremi unuttum.**
Başka bir Admin kullanıcı varsa **Kullanıcılar → Şifresini Değiştir** ile size yeni şifre verebilir. Tek Admin sizseniz programı satın aldığınız kişiye başvurun.

**Kâr neden "Tahmini" yazıyor?**
Bazı malzemelerde alış fiyatı boş bırakılmış. Özet kutusunun üzerine gelince kaç kalemin eksik olduğu görünür. Alış fiyatlarını girdiğinizde uyarı kalkar.

**Ana Ekran'daki aylık ciro ile Aylık Rapor neden farklı?**
Ana Ekran malzemeyi girildiği aya, Aylık Rapor işi başladığı aya yazar. Ay ortasında başlayıp sonraki aya taşan işlerde iki rakam farklı olur; ikisi de doğrudur.

**Yanlış bir şantiye açtım, nasıl silerim?**
Şantiye başlığına sağ tıklayın ve **Sil**'i seçin ya da kutuyu açıp **Sil** düğmesini kullanın.

**Personeli / müşteriyi silemiyorum.**
Kaydı olan personel ve müşteri, geçmiş kayıtlar bozulmasın diye silinemez. Program çıkan uyarıda **pasife almayı** önerir.

**Yevmiyeler neden kârdan düşülmüyor?**
Program puantajı yalnızca kayıt amaçlı tutar. Yevmiye toplamlarını Aylık Rapor'da ve şantiyenin Puantaj sekmesinde görebilirsiniz.

**Müşteriye verdiğim PDF'te alış fiyatlarım görünüyor mu?**
Hayır. Şantiye/servis dökümünde ve teklifte alış fiyatı, tedarikçi, kâr ve yevmiye yoktur. Bu bilgiler yalnızca size özel **Aylık Rapor** ve Excel çıktılarında bulunur.

**Teklifim neden tek sayfaya sığmıyor / yazı küçüldü?**
Bir teklifte en fazla 25 kalem olabilir; 20 kalemden sonra PDF'in tek sayfaya sığması için yazı küçülür. Daha uzun listeler için iki ayrı teklif hazırlayın.

**Bilgisayarımı değiştireceğim, verilerimi nasıl taşırım?**
Eski bilgisayarda **Ayarlar → Şimdi Yedek Al** deyin ve yedek dosyasını yeni bilgisayara kopyalayın. Yeni bilgisayarda programı kurun, ardından **Ayarlar → Yedekten Geri Yükle** ile o dosyayı seçin.

**Verilerim güvende mi?**
Veriler yalnızca kendi bilgisayarınızda durur; internete gönderilmez. Bilgisayarınıza başkasının erişmemesi için Windows hesabınıza şifre koyun ve mümkünse Windows'un **BitLocker / Cihaz şifreleme** özelliğini açın. Yedek klasörünüzü de güvenli bir yerde tutun.

---

*Spark ERP lisanslı bir yazılımdır. İzinsiz kopyalanamaz ve üçüncü kişilerle paylaşılamaz.*
*© 2026 Muhammet Akduman – Tüm hakları saklıdır.*
