package com.posed.lspd.models;

parcelable PreLoadedApk {
    List<SharedMemory> preLoadedDexes;
    List<String> moduleClassNames;
    List<String> moduleLibraryNames;
}
