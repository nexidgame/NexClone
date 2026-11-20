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

## Fitur (mengikuti desain referensi)
- Layar utama: kisi aplikasi klon, pencarian "Semua aplikasi yang dikloning", tombol ＋, ilustrasi kosong.
- **Pilih aplikasi**: riwayat penambahan + "Hapus catatan", daftar A–Z dengan indeks huruf, pencarian.
- **Ubah nama/ikon** sebelum mengkloning (tombol *Klon*) dan sesudahnya.
- Menu tahan ikon klon: **Laporan Bug**, **Pintasan Desktop**, **Ubah nama/ikon**, **Perbaiki satu kunci**, **Hapus**.
- **Pusat pengguna**: Manajemen pemberitahuan, Manajemen tugas (hentikan klon), Kunci aplikasi (sidik jari/PIN),
  Ruang Privasi (sembunyikan klon di balik verifikasi), Pertanyaan Umum, Umpan balik, Berbagi,
  Bahasa (Indonesia/English), Tentang Kami.
- Tidak disertakan: VIP/langganan, Kode F, Migrasi VIP (butuh server & penagihan).

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
