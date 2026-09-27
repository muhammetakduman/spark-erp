# Şantiye Takip – Kullanım Kılavuzu

**Sürüm 1.1.0** · Elektrik ustaları ve küçük elektrik firmaları için iş takip programı

Şantiye Takip; şantiyelerinizi, servis işlerinizi, kullandığınız malzemeleri, personel puantajını ve tahsilatlarınızı tek bir yerde tutar. Hangi işte ne kadar kazandığınızı, kimden ne kadar alacağınız kaldığını ve ay sonunda ne kazandığınızı birkaç tıkla görürsünüz.

İnternet bağlantısı gerekmez. Bütün veriler kendi bilgisayarınızda saklanır.

---

## İçindekiler

1. [Neler yapabilirsiniz?](#1-neler-yapabilirsiniz)
2. [Kurulum ve ilk açılış](#2-kurulum-ve-ilk-açılış)
3. [Önerilen ilk adımlar](#3-önerilen-ilk-adımlar)
4. [Ana Ekran](#4-ana-ekran)
5. [Şantiyeler](#5-şantiyeler)
6. [Servisler](#6-servisler)
7. [Malzeme girişi](#7-malzeme-girişi)
8. [KDV nasıl çalışır?](#8-kdv-nasıl-çalışır)
9. [Puantaj (personel çalışma günleri)](#9-puantaj-personel-çalışma-günleri)
10. [Tahsilatlar](#10-tahsilatlar)
11. [Aylık Rapor](#11-aylık-rapor)
12. [Müşteriler, Personel ve Ürünler](#12-müşteriler-personel-ve-ürünler)
13. [Ayarlar: yedekleme ve Excel](#13-ayarlar-yedekleme-ve-excel)
14. [Tutar girerken bilmeniz gerekenler](#14-tutar-girerken-bilmeniz-gerekenler)
15. [Hesaplar nasıl yapılır?](#15-hesaplar-nasıl-yapılır)
16. [Sık sorulan sorular](#16-sık-sorulan-sorular)

---

## 1. Neler yapabilirsiniz?

| Konu | Neler yapılır |
|---|---|
| **Şantiyeler** | Şantiye açma, malzeme ve puantaj girme, tahsilat kaydetme, işi bitirme, PDF döküm alma |
| **Servisler** | Tek formda servis kaydı (ücret, işçilik, değişen malzemeler), kısmi ya da tam ödeme takibi |
| **Malzeme** | Ürün kataloğu, son fiyatların otomatik önerilmesi, tedarikçi kaydı, alış ve satış KDV'si |
| **Puantaj** | Personelin hangi gün hangi işte çalıştığı, tam gün / yarım gün, yevmiye kaydı |
| **Tahsilat** | Nakit, havale, çek ve kart ile alınan ödemeler; kalan alacak |
| **Raporlar** | Aylık ve yıllık ciro, maliyet ve kâr; Excel ve PDF çıktısı |
| **Güvenlik** | Program kapanırken otomatik yedek, istenen an elle yedek, yedekten geri yükleme |

---

## 2. Kurulum ve ilk açılış

### Sistem gereksinimi
- Windows 10 veya Windows 11
- Ayrıca Java ya da başka bir program kurmanıza gerek yoktur; gereken her şey kurulumun içindedir.

### Kurulum
1. Size gönderilen **`SantiyeTakip-1.1.0.exe`** dosyasını çalıştırın.
2. Lisans sözleşmesini okuyup kabul edin.
3. İsterseniz kurulum klasörünü değiştirin, sonra **Kur**'a basın.
4. Kurulum bitince masaüstünde ve Başlat menüsünde **SantiyeTakip** kısayolu oluşur.

> Kurulumsuz sürüm aldıysanız: `SantiyeTakip` klasörünü istediğiniz yere kopyalayın ve içindeki **`SantiyeTakip.exe`** dosyasını çalıştırın.

### İlk açılış
Program ilk açıldığında lisans sözleşmesini bir kez daha gösterir. **"Lisans sözleşmesini okudum, kabul ediyorum"** kutusunu işaretleyip **Devam**'a basın. Bu ekran her sürümde yalnızca bir kez çıkar.

### Güncelleme
Yeni sürümün kurulum dosyasını çalıştırmanız yeterlidir. Eski sürümün üzerine kurulur ve **verileriniz korunur**.

---

## 3. Önerilen ilk adımlar

Programı ilk kez kullanıyorsanız şu sırayla ilerlemeniz en kolayıdır:

1. **Personel** ekranından ustalarınızı ve işçilerinizi günlük yevmiyeleriyle ekleyin.
2. **Ayarlar** ekranından yedeklerin kaydedileceği klasörü seçin. Harici disk ya da OneDrive/Google Drive klasörü önerilir.
3. **Ana Ekran**'da **+ Yeni Şantiye** ile ilk şantiyenizi açın. Müşteriyi bu pencereden de ekleyebilirsiniz.
4. Şantiyeye malzeme, puantaj ve tahsilat girin.

Ürünleri ve müşterileri önceden tek tek girmeniz gerekmez: malzeme veya iş eklerken yeni ürünü ve yeni müşteriyi aynı pencereden oluşturabilirsiniz.

---

## 4. Ana Ekran

Programın açılış ekranıdır. Üç bölümden oluşur.

### Özet kartları (en üstte)
| Kart | Ne gösterir |
|---|---|
| **Aktif Şantiye** | Devam eden şantiye sayısı |
| **Bu Ayın Kârı** | Bu ay girilen malzeme ve ücretlerden elde edilen kâr (KDV hariç) |
| **Şantiye Alacağı** | Şantiyelerden toplam alınacak para |
| **Servis Alacağı** | Ödemesi tamamlanmamış servislerden alınacak para |

Kârın altında **"Tahmini"** yazıyorsa, bazı malzemelerin alış fiyatı girilmemiştir. Bu malzemelerin maliyeti kâra yansımadığı için gösterilen kâr gerçekte olduğundan yüksek olabilir.

### "Bitmiş ama tahsilatı eksik şantiyeler"
Bitti olarak işaretlenmiş ama hâlâ parası alınmamış şantiyeler burada sarı bir kutuda listelenir. Bir satıra tıklayınca o şantiye açılır.

### Şantiyeler ve Servisler
Şantiyeler açılır-kapanır kutular halinde, servisler ise tablo halinde görünür. Ayrıntıları sonraki bölümlerdedir.

---

## 5. Şantiyeler

### Yeni şantiye açma
**Ana Ekran → + Yeni Şantiye**

| Alan | Açıklama |
|---|---|
| Müşteri | Listeden seçin ya da **Yeni Müşteri** ile ekleyin |
| Şantiye Adı | Örnek: *Ümraniye Blok B elektrik tesisatı* |
| Şantiye Adresi | İsteğe bağlı |
| Başlangıç / Bitiş Tarihi | Bitiş tarihi boş bırakılabilir |
| Durum | **Aktif** veya **Bitti** |
| Servis Ücreti / İşçilik Ücreti | Varsa sabit ücretler ve KDV seçimleri |

### Şantiye kutusu
Her şantiyenin başlığında **müşteri – şantiye adı**, durumu (Aktif / Bitti) ve sağda **kalan alacak** yazar. Başlığa tıklayınca kutu açılır:

- **Üst satır (işlemler):**
  **Malzeme Ekle** · **Tahsilat Ekle** · **Puantaj Gir** … **Düzenle** · **PDF Al** · **Sil**
- **Özet kutuları:**
  - **Toplam (KDV dahil)**: altında KDV'siz tutar ve KDV ayrı ayrı yazar
  - **Tahsil Edilen**
  - **Kalan**: kırmızıysa alacak var, yeşilse ödeme tamam
  - **Kâr (KDV hariç)**
- **Sekmeler:** **Malzemeler**, **Puantaj**, **Tahsilatlar**

Başlıkların üstündeki arama kutusuna müşteri ya da şantiye adı yazarak arama yapabilirsiniz. **Sadece aktif** kutusunun işaretini kaldırırsanız bitmiş şantiyeler de listelenir.

### Şantiyeyi bitirme
**Düzenle** ile açıp **Durum**'u **Bitti** yapın. Şantiyede kalan alacak varsa program sizi uyarır. Yine de devam ederseniz şantiye, Ana Ekran'daki "tahsilatı eksik" listesine düşer.

### Şantiyeyi silme
Yanlış açılmış bir şantiyeyi, kutunun içindeki **Sil** butonuyla ya da başlığa **sağ tıklayıp → Sil** diyerek silebilirsiniz.

> ⚠️ Silme işlemi geri alınamaz. Şantiyeyle birlikte malzeme, puantaj ve tahsilat kayıtları da silinir.

### PDF döküm
**PDF Al**, müşteriye verebileceğiniz bir döküm hazırlar: müşteri bilgileri, malzeme listesi, ücretler, KDV'li toplamlar ve tahsilatlar. **Alış fiyatları, tedarikçiler ve kârınız bu belgede yer almaz.**

---

## 6. Servisler

Kısa süreli arıza ve tamir işleri içindir.

### Yeni servis
**Ana Ekran → + Yeni Servis**. Tek bir formda şunları girersiniz:
- Müşteri, tarih, açıklama (örn. *Pano arızası*)
- Servis ücreti ve işçilik ücreti (KDV seçimleriyle)
- **+ Malzeme Ekle** ile değişen parçalar

Formun altında **Ara toplam / KDV / Genel toplam** siz yazdıkça anında hesaplanır.

### Ödeme
- Paranın tamamını aldıysanız **"Ödemenin tamamı alındı"** kutusunu işaretleyin.
- Parça parça ödeme aldıysanız servisi düzenlerken **Tahsilatlar** bölümünden tek tek ekleyin. Kalan tutar otomatik hesaplanır.

### Servis tablosu
- Üstteki **Tümü / Alınmadı / Alındı** düğmeleriyle filtreleyebilirsiniz.
- Ödemesi tamamlananlar yeşil, bekleyenler kırmızı zeminle görünür.
- **Tamamı Alındı** sütunundaki kutuyla ödemeyi doğrudan tablodan işaretleyebilirsiniz.
- **Çift tıklama** servisi düzenler. **Sağ tık** menüsünde Düzenle, Tahsilat Ekle, Puantaj Gir, PDF Al ve Sil bulunur.

---

## 7. Malzeme girişi

**Malzeme Ekle** penceresi şantiye ve servislerde aynıdır.

| Alan | Açıklama |
|---|---|
| **Tarih** | Malzemenin verildiği gün |
| **Ürün** | Yazmaya başlayın, katalogdan eşleşenler listelenir. Katalogda yoksa **"+ '…' ürününü ekle"** bağlantısıyla oradan ekleyin |
| **Miktar** | Örn. 200 (metre), 2,5 (kg) |
| **Satış Fiyatı** | **Birim fiyat** *veya* **Toplam tutar** girin, diğeri kendiliğinden hesaplanır |
| **Satış KDV** | Bkz. [KDV](#8-kdv-nasıl-çalışır) |
| **Alış Fiyatı** | İsteğe bağlı ama **kârın doğru hesaplanması için önerilir**; kendi KDV seçimi vardır |
| **Tedarikçi** | Daha önce yazdığınız tedarikçiler önerilir. Yeni bir ad da yazabilirsiniz; ilk kayıttan sonra o da listeye eklenir |
| **Not** | İsteğe bağlı |

**Akıllı öneriler:** Bir ürünü seçtiğinizde, o ürünün son alış fiyatı, satış fiyatı, tedarikçisi ve KDV seçimi otomatik doldurulur. Değiştirmek isterseniz üzerine yazmanız yeterlidir.

**Kaydet ve yeni ekle** ile pencereyi kapatmadan art arda malzeme girebilirsiniz. Eklediklerinizi pencerenin altındaki listede görürsünüz.

Girilmiş bir malzemeyi düzeltmek için **Malzemeler** sekmesinde satıra çift tıklayın ya da seçip **Düzenle**'ye basın.

> "Tedarikçi" alanında büyük/küçük harf ve Türkçe karakter farkı önemsizdir: *"Elektrik Market"* ile *"elektrik market"* aynı tedarikçi sayılır ve listede bir kez görünür.

---

## 8. KDV nasıl çalışır?

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
- Şantiye ve servis özetinde toplamın altında **KDV'siz tutar + KDV** ayrıca yazar.
- **Kâr her zaman KDV hariç tutarlar üzerinden hesaplanır.** Kalan alacak ise müşterinin ödeyeceği KDV dahil tutardır.

---

## 9. Puantaj (personel çalışma günleri)

Hangi personelin hangi gün hangi işte çalıştığını kaydeder.

> **Önemli:** Puantaj yalnızca **kayıt amaçlıdır**. Yevmiyeler şantiye kârından ve aylık kârdan **düşülmez**. Raporlarda bilgi olarak ayrı gösterilir.

### Puantaj girme
Şantiye kutusunda **Puantaj Gir**'e basın. Servisler için bu seçenek sağ tık menüsündedir.

1. **Tek gün** veya **Tarih aralığı** seçin. Aralıkta istenirse **Cumartesi atla / Pazar atla** işaretlenir.
2. Çalışan personeli işaretleyin. Yevmiye, personelin kayıtlı yevmiyesiyle gelir ve değiştirilebilir.
3. **Katsayı**: **1,0** tam gün, **0,5** yarım gün.
4. Pencerenin altında özet görünür, örn. *3 tarih × 2 kişi = 6 gün*. **Kaydet**'e basın.

Program sizi şu durumlarda uyarır:
- Personel aynı gün **başka bir işte** de kayıtlıysa
- Bir personelin aynı gündeki toplamı **1 günü aşıyorsa**
- Tarih, işin başlangıç–bitiş aralığının dışındaysa

Aynı kişiyi aynı işe aynı gün ikinci kez eklerseniz tekrar kayıt oluşmaz, o kayıt atlanır.

### Puantaj sekmesi
Önce personel bazında özet görünür: gün sayısı, ortalama yevmiye, toplam ve çalıştığı günler. Altında **günlük kayıtlar** yer alır. Yevmiye, katsayı veya açıklamayı düzeltmek için hücreye çift tıklayın. Bir satırı silmek için seçip **Sil**'e basın.

### Personel Puantajı ekranı
Sol menüden **Personel Puantajı**: bir personel ve tarih aralığı seçin.
- **İş Bazında**: hangi şantiye veya serviste kaç gün çalıştığı ve toplam
- **Takvim**: gün gün tablo; **Excel'e Aktar** ile dosyaya alınabilir

---

## 10. Tahsilatlar

**Tahsilat Ekle** ile müşteriden alınan her ödemeyi girin: tarih, tutar, yöntem (**Nakit / Havale / Çek / Kart**) ve isteğe bağlı not.

- **Tahsilatlar** sekmesinde tüm ödemeler listelenir. Çift tıklama düzenler, **Sil** kaldırır.
- **Kalan = KDV dahil toplam − tahsil edilenler.**
- Müşteri toplamdan fazla ödediyse kalan eksiye düşmez; ödeme **"Tahsilat tamam"** olarak görünür.

---

## 11. Aylık Rapor

"Bu ay ne kazandım?" sorusunun cevabıdır. Sol menüden **Aylık Rapor**'u açın.

### Tarih kuralı
Her iş, **kendi tarihinin ayına** yazılır: şantiyede başlangıç tarihi, serviste servis tarihi. Eylülde yaptığınız bir işin parasını ekimde alsanız da o iş **Eylül** cirosunda görünür. Henüz alınmayan para **Tahsil Edilmeyen** olarak ayrıca gösterilir.

### Aylık sekme
- **Ay seçimi:** ◀ ▶ okları, ay ve yıl kutuları, **Bu ay** düğmesi
- **Filtre:** Tümü / Sadece şantiyeler / Sadece servisler
- **Kartlar:**
  - **Ciro (KDV hariç)**: altında KDV tutarı bilgi olarak yazar
  - **Maliyet**: malzeme alışları; altında o ayın yevmiye toplamı bilgi olarak yazar
  - **Kâr**: ciro − maliyet; alış fiyatı eksikse "Tahmini" olarak işaretlenir
  - **Tahsil Edilmeyen**
- **İş tablosu:** Tarih, Müşteri, İş (ŞANTİYE / SERVİS etiketiyle), Malzeme, Yevmiye, Ciro, Maliyet, Kâr, Tahsilat. En altta **TOPLAM** satırı vardır. Bir satıra çift tıklayınca o iş Ana Ekran'da açılır.
- **Malzeme özeti:** o ay en çok kullanılan ürünler, toplam miktar, alış ve satış
- **Yevmiye özeti:** personel bazında gün sayısı ve yevmiye toplamı
- **Excel'e Aktar** ve **PDF Al** ile raporu dosyaya kaydedebilirsiniz.

### Yıllık sekme
Seçtiğiniz yılın 12 ayı tek tabloda görünür: Ay, Ciro, Maliyet, Kâr, Tahsil Edilmeyen ve yıl toplamı. Altında **aylık kâr grafiği** bulunur.

---

## 12. Müşteriler, Personel ve Ürünler

### Müşteriler
Ad Soyad / Firma, telefon, adres, vergi no ve not. Telefon ve adres, şantiye kutusunun üstünde görünür; böylece müşteriyi hemen arayabilirsiniz. İşi olan bir müşteri silinemez.

### Personel
Ad, günlük yevmiye, **Usta** işareti ve **Aktif** durumu. Puantaj kaydı olan personel silinemez; program bunun yerine **pasife almayı** önerir. Pasif personel puantaj ekranında varsayılan olarak listelenmez.

### Ürünler
Katalogdaki ürünler ve birimleri (Adet, Metre, Kg, Takım, Paket). Soldan bir ürün seçince sağda **kullanım geçmişi** açılır:
- Ürünün kime, ne zaman, kaç tane ve kaça verildiği
- En düşük, en yüksek ve son satış fiyatı
- **En ucuz tedarikçi**
- Tarih filtresi ve **Excel'e Aktar**

Bir satıra çift tıklarsanız ilgili iş açılır.

---

## 13. Ayarlar: yedekleme ve Excel

### Yedekleme
- **Otomatik yedek:** Program her kapandığında verilerinizin bir yedeğini alır. En son **30 yedek** saklanır, daha eskileri silinir.
- **Yedek Klasörü:** Varsayılan olarak `%APPDATA%\SantiyeTakip\yedekler` klasörüdür. **Klasör Seç** ile değiştirebilirsiniz.
- **Şimdi Yedek Al:** O anda elle yedek alır.
- **Yedekten Geri Yükle:** Seçtiğiniz yedeği geri yükler. Mevcut veriler yedektekilerle değişir ve program kapanır; sonra yeniden açmanız gerekir.

> 💡 Bilgisayar arızasına karşı yedek klasörünü **harici disk** ya da **bulut klasörü** (OneDrive, Google Drive) yapmanızı öneririz.

### Excel Dışa Aktarım
Başlangıç ve bitiş tarihi seçip **Excel'e Aktar**'a basın. O tarihlerdeki tüm işler, satış, KDV, maliyet, kâr ve tahsilat bilgileriyle tek bir tabloya aktarılır.

### Verilerin yeri
Tüm veriler tek bir dosyada tutulur: `%APPDATA%\SantiyeTakip\veri.db`. Bu dosyayı elle silmeyin veya taşımayın; yedek ve geri yükleme için **Ayarlar** ekranını kullanın.

---

## 14. Tutar girerken bilmeniz gerekenler

Bütün tutar ve miktar kutuları aynı şekilde çalışır. Böylece her yerde aynı yazım görürsünüz:

- Sadece **rakam** ve **ondalık için virgül** yazılır. Harf yazılamaz.
- **Binlik noktalarını siz koymazsınız**, program yazarken kendisi ekler: `1000` yazınca `1.000` olur.
- Kutudan çıkınca tutar **`1.000,00`** biçimine getirilir.
- Nokta tuşu yok sayılır; ondalık için **virgül** kullanın. `12.5` yazarsanız `125` olur, 12,5 için `12,5` yazın.
- Kopyalayıp yapıştırdığınız `1.250,00 ₺` veya `1000.50` gibi tutarlar doğru okunur.
- Tarihler **gg.aa.yyyy** biçimindedir (örn. 25.09.2026).

---

## 15. Hesaplar nasıl yapılır?

| Terim | Hesap |
|---|---|
| **Satış / Ciro** | Malzeme satışları + servis ücreti + işçilik ücreti, **KDV hariç** |
| **Genel toplam** | Satış + KDV (müşterinin ödeyeceği tutar) |
| **Maliyet** | Malzemelerin **alış** fiyatları, KDV hariç |
| **Kâr** | Ciro − Maliyet |
| **Kalan** | Genel toplam − Tahsil edilenler (eksiye düşmez) |
| **Yevmiye** | Günlük ücret × katsayı (tam gün 1, yarım gün 0,5); **kâra dahil edilmez** |
| **"Tahmini" kâr** | Bazı malzemelerin alış fiyatı girilmemiş; o kalemlerin maliyeti eksik |

---

## 16. Sık sorulan sorular

**Kâr neden "Tahmini" yazıyor?**
Bazı malzemelerde alış fiyatı boş bırakılmış. Özet kutusunun üzerine gelince kaç kalemin eksik olduğu görünür. Alış fiyatlarını girdiğinizde bu uyarı kalkar.

**Yanlış bir şantiye açtım, nasıl silerim?**
Şantiye başlığına sağ tıklayın ve **Sil**'i seçin. İsterseniz kutuyu açıp **Sil** butonunu da kullanabilirsiniz.

**Personeli silemiyorum.**
Puantaj kaydı olan personel, geçmiş kayıtlar bozulmasın diye silinemez. Program çıkan uyarıda personeli **pasife almayı** önerir.

**Yevmiyeler neden kârdan düşülmüyor?**
Program puantajı yalnızca kayıt amaçlı tutar. Yevmiye toplamlarını Aylık Rapor'da ve şantiyenin Puantaj sekmesinde görebilirsiniz.

**Bilgisayarımı değiştireceğim, verilerimi nasıl taşırım?**
Eski bilgisayarda **Ayarlar → Şimdi Yedek Al** deyin ve yedek dosyasını yeni bilgisayara kopyalayın. Yeni bilgisayarda programı kurun, ardından **Ayarlar → Yedekten Geri Yükle** ile o dosyayı seçin.

**PDF'te alış fiyatlarım görünüyor mu?**
Hayır. Müşteriye verilen PDF dökümde alış fiyatı, tedarikçi ve kâr bilgisi yoktur. Bu bilgiler yalnızca size özel **Aylık Rapor** PDF'inde bulunur.

---

*Şantiye Takip lisanslı bir yazılımdır. İzinsiz kopyalanamaz ve üçüncü kişilerle paylaşılamaz.*
*© 2026 Muhammet Akduman – Tüm hakları saklıdır.*
