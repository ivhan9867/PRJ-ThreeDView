# 3D Visual Realism Suite (Grafika Komputer - Minggu 11)
**Implementasi Rendering Faces pada Visual Realism berbasis Java OOP & Modern OpenGL (GLSL 330 core)**

Proyek ini merupakan pembaruan dari modul perkuliahan dan praktikum grafika komputer Minggu ke-11. Proyek memperluas aplikasi sebelumnya menjadi *Visual Realism Suite* yang mengimplementasikan 5 pilar utama rendering permukaan objek 3D (*rendering faces*) sesuai dengan Sub-CPMK / Indikator Penilaian.

---

## Fitur & Pemenuhan Indikator Penilaian (Bobot 100%)

| Indikator | Fitur yang Diimplementasikan | Kontrol Keyboard | Keterangan Teknis |
| :--- | :--- | :---: | :--- |
| **Indikator 1 (20%)** | **Shading Models Switcher** | `Key 'B'` | Beralih antara **Blinn-Phong** (Halfway Vector $H$) dan **Standard Phong** (Reflection Vector $R$) pada Fragment Shader. |
| **Indikator 2 (20%)** | **Flat vs Smooth Shading** | `Key 'F'` | Beralih antara **Flat Shading** (facet normal konstan per-segitiga) dan **Smooth Shading** (*vertex normal averaging* tanpa efek *Mach Banding*). |
| **Indikator 3 (20%)** | **Hidden Surface Removal (HSR)** | `Key 'Z'` & `Key 'C'` | `Z`: Toggle **Z-Buffer Depth Test** (`glEnable/glDisable(GL_DEPTH_TEST)`).<br>`C`: Toggle **Back-Face Culling** (`glEnable/glDisable(GL_CULL_FACE)`). |
| **Indikator 4 (20%)** | **Texture Mapping to Faces** | `Key 'T'` & `Key 'M'` | `T`: Toggle pemetaan tekstur ON/OFF.<br>`M`: Toggle Filtering (**Nearest Neighbor** vs **Bilinear Mipmapped**). Menggunakan STBImage & sampler GLSL. |
| **Indikator 5 (20%)** | **Real-Time Shadow Mapping** | `Key 'X'` | Algoritma Two-Pass Shadow Mapping dengan Depth Framebuffer Object (FBO 2048x2048), *dynamic slope-scaled bias* (anti-*acne*), dan *PCF 3x3 filter* (bayangan lembut). |
| **Spesifikasi Tambahan** | **Model 3D Kompleks** | `Key '1'` - `'4'` | `1`: UV Sphere (Bola 3D)<br>`2`: Rumah 3D (House 3D bermateri: atap, cerobong, dinding)<br>`3`: Torus (Donat 3D)<br>`4`: Color/Textured Cube |
| **Pencahayaan Dinamis** | **Orbiting Light Source** | `Key 'L'` | Menggerakkan sumber cahaya berotasi mengelilingi objek secara real-time untuk melihat pergerakan bayangan dinamis. |
| **Navigasi Kamera** | **Orbital Camera & FOV** | Mouse / Panah | **Drag Klik Kiri**: Orbit kamera.<br>**Scroll**: Zoom in / Zoom out.<br>**Panah Atas / Bawah**: Atur FOV.<br>`R`: Reset posisi kamera & pengaturan. |

---

## Struktur Direktori & Kelas OOP Modular

```
ThreeDViewing_NB/
├── pom.xml                                 # Konfigurasi Maven (LWJGL 3.3.3, JOML, STB)
├── nbactions.xml                           # Konfigurasi Run / Debug Apache NetBeans
├── README.md                               # Panduan Proyek & Dokumentasi
└── src/
    └── main/
        ├── java/com/grafkom/viewing3d/
        │   ├── ThreeDViewing.java          # Main Application, Render Loop & GLFW Window Manager
        │   ├── Camera3D.java               # Orbital Camera (Basis u, v, n & LookAt Manual)
        │   ├── PerspectiveProjection.java  # Proyeksi Perspektif Terenkapsulasi
        │   ├── HSRManager.java             # Praktikum 2: Pengaturan Z-Buffer & Back-Face Culling
        │   ├── Texture2D.java              # Praktikum 3: STBImage Texture Loader & Filter Mode
        │   ├── ShadowMapFBO.java           # Praktikum 4: Depth Framebuffer Object Manager
        │   ├── Shader.java                 # GLSL Shader Program Compiler, Linker & Uniforms
        │   └── Mesh3D.java                 # Geometri 3D (Sphere, House 3D, Torus, Cube, Ground Plane)
        └── resources/
            ├── shaders/
            │   ├── phong.vert              # Vertex Shader (Model, Normal Matrix, Light-Space)
            │   ├── phong.frag              # Fragment Shader (Blinn-Phong, PCF Shadow, Texture)
            │   ├── shadowDepth.vert        # Pass 1 Vertex Shader (Light Space Transform)
            │   └── shadowDepth.frag        # Pass 1 Fragment Shader (Depth Pass)
            └── textures/
                ├── stone_brick.png         # Tekstur Dinding Batu Bata
                └── wood_crate.png          # Tekstur Peti Kayu
```

---

## Cara Menjalankan Proyek

### 1. Menjalankan di Apache NetBeans IDE
1. Ekstrak file zip ke komputer Anda.
2. Buka NetBeans, pilih **File > Open Project**.
3. Pilih folder `ThreeDViewing_NB` (ikon folder akan berubah menjadi ikon proyek Maven).
4. Tunggu NetBeans memuat dependensi Maven.
5. Klik kanan pada proyek > pilih **Run** (atau tekan `F6`).

### 2. Menjalankan Melalui Terminal / Command Prompt (Maven CLI)
Buka terminal di dalam direktori folder `ThreeDViewing_NB`:
```bash
# Kompilasi kode sumber
mvn clean compile

# Jalankan aplikasi
mvn exec:java
```

---

## Panduan Pengujian 5 Indikator CPMK untuk Laporan Praktikum
1. **Pengujian Phong vs Blinn-Phong (Indikator 1)**:
   - Tekan tombol `B` untuk beralih antara Phong dan Blinn-Phong.
   - Amati bentuk *specular highlight* pada model Bola (Sphere) atau Torus. Blinn-Phong menghasilkan kilauan yang lebih halus dan lebih eliptis saat sudut pandang miring.
2. **Pengujian Flat vs Smooth Shading (Indikator 2)**:
   - Tekan tombol `F` untuk beralih antara Flat dan Smooth.
   - Pada Flat Shading, facet poligon segitiga dan efek *Mach Banding* tampak jelas. Pada Smooth Shading, *vertex normal averaging* membuat permukaan kurva bola menjadi sangat halus.
3. **Pengujian Hidden Surface Removal (Indikator 3)**:
   - Tekan tombol `Z` untuk mematikan/menyalakan Z-Buffer. Saat Z-Buffer dimatikan (`OFF`), fragment belakang akan menimpa bagian depan (*depth overlapping artifact*).
   - Tekan tombol `C` untuk mematikan/menyalakan Back-Face Culling.
4. **Pengujian Texture Mapping & Filtering (Indikator 4)**:
   - Tekan tombol `T` untuk menyalakan/mematikan tekstur.
   - Tekan tombol `M` untuk beralih antara *Nearest Neighbor* dan *Bilinear Mipmapped*. Amati perbedaan kejernihan dan ketajaman tekstur pada jarak dekat maupun jauh.
5. **Pengujian Real-Time Shadow Mapping (Indikator 5)**:
   - Tekan tombol `X` untuk mengaktifkan/menonaktifkan bayangan.
   - Tekan tombol `L` untuk mengaktifkan animasi rotasi sumber cahaya. Bayangan objek 3D akan memanjang dan berputar di atas lantai (*ground plane*) secara presisi real-time.
