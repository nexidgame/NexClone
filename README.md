# Nex Clone

Aplikasi clone apps untuk Android 11 – 16 (`com.nex.clone`): jalankan dua akun dari aplikasi yang sama
(WhatsApp, Instagram, game, dll.) dengan data terpisah. Tema putih dengan biru soft.

## Cara kerja
Nex Clone memakai **profil kerja (work profile) bawaan Android**, bukan mesin virtual.
Nex Clone menjadi pengelola profil itu (Device Policy Controller / profile owner), lalu memasang ulang
aplikasi yang dipilih di dalam profil tersebut (`DevicePolicyManager.installExistingPackage`).
Aplikasi dibuka lewat `LauncherApps` (cadangan: perintah lintas profil).

Dua salinan Nex Clone terpasang: satu di profil utama (yang Anda pakai) dan satu di profil klon
(tanpa ikon, hanya menerima perintah lewat cross-profile intent `com.nex.clone.action.PROFILE_OP`).

## Alur pemakaian
1. **Pertama kali dibuka**, Nex Clone langsung membuat profil kerja (layar persetujuan sistem akan muncul sekali).
2. **Beranda** menampilkan semua aplikasi bawaan ponsel. Yang sudah ada di profil kerja diberi lencana koper.
   Ketuk = buka (salinan profil kerja bila ada, kalau tidak aplikasi aslinya).
3. **Menginstal aplikasi ke profil kerja**: ketuk **＋** lalu pilih aplikasi (bisa ubah nama/ikon dulu), atau
   **tahan** ikon di beranda lalu pilih **Install ke profil kerja**. Aplikasinya lalu muncul di profil kerja.
4. **Di dalam profil kerja** (tab Kerja di peluncur), Nex Clone hanya menampilkan dua menu:
   **Perbaiki** (jalankan ulang penyiapan) dan **Hapus profil kerja**.

## Fitur (mengikuti desain referensi)
- Pilih aplikasi: riwayat penambahan + "Hapus catatan", daftar A–Z dengan indeks huruf, pencarian.
- Menu tahan pada aplikasi di profil kerja: Laporan Bug, Pintasan Desktop, Ubah nama/ikon, Perbaiki satu kunci, Hapus.
- Pusat pengguna: Manajemen pemberitahuan, Manajemen tugas, Kunci aplikasi, Ruang Privasi, Pertanyaan Umum,
  Umpan balik, Berbagi, Bahasa (Indonesia/English), Tentang Kami.
- Pesan galat spesifik bila perintah ke profil kerja tidak sampai (NOT_FOUND / CANCELED / FAILED).
- Tidak disertakan: VIP/langganan, Kode F, Migrasi VIP (butuh server & penagihan).

## Cara instal ke profil kerja (bertingkat, mirip "Clone" di Island)
Dijalankan oleh Nex Clone di dalam profil kerja, berurutan sampai ada yang berhasil:
1. `installExistingPackage` — memasang salinan dari paket yang sudah ada di ponsel (tanpa unduh).
2. `enableSystemApp` — untuk aplikasi sistem.
3. Melepas pembatasan instal lalu mengulang langkah 1.
4. `PackageInstaller` dari APK sumber (base + split) — muncul layar konfirmasi sistem di profil kerja.
5. Membuka Play Store di profil kerja agar Anda menginstal manual.
Bila gagal, dialog menampilkan alasan teknisnya (untuk diagnosis).

## Jika muncul "profil kerja belum siap" / gagal memasukkan aplikasi
Perintah dari profil utama dikirim ke Nex Clone di profil kerja lewat cross-profile intent. Bila tidak sampai:
- Buka Nex Clone di **tab Kerja** lalu ketuk **Perbaiki** (menjalankan ulang pendaftaran izin lintas profil).
- Setelah memperbarui APK, penyiapan profil yang sudah ada diperbaiki otomatis (penerima `MY_PACKAGE_REPLACED`).
- Izin lintas profil kini dipasang dua arah (`FLAG_PARENT_CAN_ACCESS_MANAGED | FLAG_MANAGED_CAN_ACCESS_PARENT`).

## Build
Butuh JDK 17 dan Android SDK 36.
```
gradle wrapper --gradle-version 8.11.1   # sekali saja
./gradlew assembleDebug
```
Atau upload ke GitHub: workflow `.github/workflows/build.yml` membuat APK otomatis (Actions → Artifacts).

## Cara pakai
1. Pasang APK, buka Nex Clone, ketuk **Siapkan sekarang**, lalu ikuti layar persetujuan sistem (membuat profil kerja).
2. Ketuk **＋**, pilih aplikasi, ubah nama/ikon bila perlu, ketuk **Klon**.
3. Buka klon dari kisi di layar utama. Login dengan akun kedua.

## Batasan yang perlu diketahui
- Perangkat harus mengizinkan profil kerja. Beberapa merek/ROM menonaktifkannya, atau sudah ada profil kerja
  dari aplikasi lain (mis. Shelter/Island) — maka penyiapan ditolak.
- Klon tampil dengan lencana koper dan juga terlihat di peluncur ponsel; ini perilaku Android.
- Kunci aplikasi dan Ruang Privasi hanya berlaku untuk pembukaan lewat Nex Clone/pintasannya.
- Menghapus profil kerja menghapus data semua klon.
- Aplikasi yang melarang berjalan di profil kerja bisa gagal berjalan.
- Perintah lintas profil bisa dikirim oleh aplikasi lain di profil utama (tidak ada autentikasi tambahan);
  perintah terbatas pada profil klon saja.

Lisensi: MIT
