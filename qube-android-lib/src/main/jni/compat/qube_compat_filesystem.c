
#include <jni.h>
#include <stdio.h>
#include "qube_logutils.h"
#include <dlfcn.h>
#include <fcntl.h>
#include <errno.h>
#include <malloc.h>
#include <signal.h>
#include <stdlib.h>
#include <unistd.h>
#include <dlfcn.h>
#include <unwind.h>
#include <dlfcn.h>
#include <sys/stat.h>
#include "qube_compat_filesystem.h"
#include "qube_compat.h"
#include <errno.h>
#include <string.h>

// QEMU's generic block layer (image locking / probing) calls stat()/open()
// on the raw "-drive file=..." value BEFORE the vvfat block driver gets a
// chance to parse and strip its own "fat:" / "rw:" / "ro:" protocol prefix.
// Without stripping it here too, every lookup below sees a bogus path like
// "fat:rw:/storage/emulated/0/dm" instead of the real directory, and fails.
static const char* strip_vvfat_prefix(const char* path) {
    if (!strncmp(path, "fat:", 4)) {
        path += 4;
        if (!strncmp(path, "rw:", 3))
            path += 3;
        else if (!strncmp(path, "ro:", 3))
            path += 3;
    }
    return path;
}

#if ENABLE_ASF
int close_fd(int fd) {

	pthread_mutex_lock(&fd_lock);

	JNIEnv *env;
	jmethodID methodID;
	jint jfd, jres;
	int res = -1;

	if (jvm == NULL) {
		LOGE("Jvm not initialized");
		return -1;
	}
	jint rs = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
	if (rs != JNI_OK) {
		LOGE("Could not get env");
		return -1;
	}

	jclass cls = (*env)->GetObjectClass(env, jobj);
	methodID = (*env)->GetMethodID(env, cls, "close_fd", "(I)I");
	jres = (jint)(*env)->CallIntMethod(env, jobj, methodID, fd);

	res = (int) jres;

	(*jvm)->DetachCurrentThread(jvm);
	pthread_mutex_unlock(&fd_lock);
	return res;

}
int get_fd(const char * filepath) {
	pthread_mutex_lock(&fd_lock);
	int fd = -1;

	JNIEnv *env;
	jmethodID methodID;
	jint jfd;

	//LOGI("get_fd: %s", filepath);

	if (jvm == NULL) {
		LOGE("Jvm not initialized");
		return -1;
	}
	jint rs = (*jvm)->AttachCurrentThread(jvm, &env, NULL);
	if (rs != JNI_OK) {
		LOGE("Could not get env");
		return -1;
	}
	jstring jfilepath = (*env)->NewStringUTF(env, filepath);

	jclass cls = (*env)->GetObjectClass(env, jobj);
	methodID = (*env)->GetMethodID(env, cls, "get_fd", "(Ljava/lang/String;)I");
	jfd = (jint)(*env)->CallIntMethod(env, jobj, methodID, jfilepath);

	fd = (int) jfd;

	(*env)->DeleteLocalRef(env, jfilepath);

	(*jvm)->DetachCurrentThread(jvm);
	pthread_mutex_unlock(&fd_lock);
	//LOGI("Opened file: %d, %s", fd, filepath);

	return fd;
}

#endif

FILE* android_fopen(const char *path, const char *mode) {

	FILE* f = NULL;
	int fd = 0;

	path = strip_vvfat_prefix(path);

	//printf("fopen file: %s, %s", path, mode);
    ////printf("fopen: Qube base dir length: %d", strlen(qube_base_dir));


    #ifdef ENABLE_ASF
        if(!strncmp(path,"/content/",9)){
            fd = create_thread_get_fd(path);
            f=fdopen(fd, mode);
            //printf("fdopen ASF file: %s, %s, %p", path, mode, f);
        	return f;
        }
    #endif

        if(!strncmp(path,"/content/",9)){
            LOGE("Cannot open file %s, enable SDCard API in Config.java and reopen your file", path);
            return f;
        }

        //First we try to load the file path as is but only if it's under the qube folder or storage
        //if(!strncmp(path,qube_base_dir, strlen(qube_base_dir))
        //    || !strncmp(path,storage_base_dir, strlen(storage_base_dir))
       // ){
            f = fopen(path,mode);
            //printf("fopen absolute filepath: %s, %s, %p", path, mode, f);
            if(f)
                return f;
        //}

        //Otherwise we prefix and try under the internal storage
        char lpath[256];
        strcpy(lpath, storage_base_dir);
        strcat(lpath, path);
        f = fopen(lpath,mode);
        //printf("fopen storage file: %s, %s, %p", lpath, mode, fd);
        if(f)
            return f;

        //finally we prefix and look under the qube dir
        strcpy(lpath, qube_base_dir);
        strcat(lpath, path);
        f = fopen(lpath,mode);

        if(!f)
            LOGW("Could not fopen: %s, %s, %p", lpath, mode, f);

    return f;
}

int android_open(const char *path, int flags, ...) {

	int fd=-1;
    int mode = 0;

	va_list ap;
    path = strip_vvfat_prefix(path);
    //printf("Open file: %s, %d, %d", path, flags, mode);
    ////printf("open: Qube base dir length: %d", strlen(qube_base_dir));

    if (flags & O_CREAT) {
        va_start(ap, flags);
        mode = va_arg(ap, int);
        va_end(ap);
    }

#ifdef ENABLE_ASF
    if(!strncmp(path,"/content/",9)){
        fd = create_thread_get_fd(path);
        //printf("Open ASF file: %s, %d, %d, %d", path, flags, mode, fd);
    	return fd;
    }
#endif

    if(!strncmp(path,"/content/",9)){
        LOGW("Cannot open file %s, enable SDCard API in Config.java and reopen your file", path);
        return fd;
    }

    //First we try to load the file path as is but only if it's under the qube folder or storage
    //if(!strncmp(path,qube_base_dir, strlen(qube_base_dir))
    //    || !strncmp(path,storage_base_dir, strlen(storage_base_dir))
    //){
        fd = open(path,flags,mode);
        //printf("Opening absolute filepath: %s, %d, %d, %d", path, flags, mode, fd);
        if(fd>0)
            return fd;
    //}

    //Otherwise we prefix and try under the internal storage
    char lpath[256];
    strcpy(lpath, storage_base_dir);
    strcat(lpath, path);
    fd = open(lpath,flags,mode);
    //printf("Open storage file: %s, %d, %d, %d", lpath, flags, mode, fd);
    if(fd>0)
        return fd;

    //finally we prefix and look under the qube dir
    strcpy(lpath, qube_base_dir);
    strcat(lpath, path);
    fd = open(lpath,flags,mode);

    if(!fd)
        LOGW("Could not open file: %s, %d, %d, %d", lpath, flags, mode, fd);

    return fd;
}

int android_mkstemp(char * path){

    int res;

    // QEMU generates its own temp file paths (e.g. the read-write overlay
    // vvfat needs for "fat:rw:" shares) as already-absolute paths under the
    // app's own private cache dir. Blindly prefixing those with
    // qube_base_dir produces a nonexistent nested path and breaks mkstemp,
    // which in turn breaks the whole Shared Folder feature. Try the path
    // as-is first, just like android_fopen/android_open/android_stat do.
    if (path[0] == '/') {
        res = mkstemp(path);
        if (res >= 0)
            return res;
        LOGW("Could not create file: %s", path);
        return res;
    }

    char lpath[256];
    strcpy(lpath, qube_base_dir);
    strcat(lpath, path);
    res = mkstemp(lpath);
    //printf("mkstemp qube file: %s", path);
    if(res < 0)
        LOGW("Could not create file: %s", lpath);
    return res;

}

int android_close(int fd) {
	int res = 0;

	printf("Closing fd: %d", fd);
#ifdef ENABLE_ASF
	res = create_thread_close_fd(fd);
#else
	res = close(fd);
#endif
	return res;
}

int android_stat(const char* path, struct stat* val){

	int res=-1;
    path = strip_vvfat_prefix(path);
    //printf("Stat file: %s", path);
    //printf("stat: qube base dir length: %d", strlen(qube_base_dir));


#ifdef ENABLE_ASF
    if(!strncmp(path,"/content/",9)){
        int fd = create_thread_get_fd(path);
        res = fstat(fd,val);
        //printf("stat ASF file: %s, %d", path, res);
        return res;
    }
#endif

    if(!strncmp(path,"/content/",9)){
        printf("Cannot stat ASF file %s, not supported", path);
        return -1;
    }

    //First we try to stat the file path as is but only if it's under the qube folder or storage
    if(!strncmp(path,qube_base_dir, strlen(qube_base_dir))
        || !strncmp(path,storage_base_dir, strlen(storage_base_dir))
    ){
        res = stat(path,val);
        //printf("stat absolute filepath: %s, %d", path, res);
        if(!res)
            return res;
        else {
            //printf("Stat file ERROR: %s : %d", path, errno);
        }
    }

    //Otherwise we try under the internal storage
    char lpath[256];
    strcpy(lpath, storage_base_dir);
    strcat(lpath, path);
    res = stat(lpath,val);
    //printf("stat storage file: %s, %d", lpath, res);
    if(!res)
        return res;
    else {
        //printf("Stat file ERROR: %s : %d", lpath, errno);
    }

    //finally we look under the qube dir
    strcpy(lpath, qube_base_dir);
    strcat(lpath, path);
    res = stat(lpath,val);
    //printf("stat qube file: %s, %d", lpath, res);
    if(res < 0)
        LOGW("Could not stat file: %s : %d", lpath, errno);

    return res;
}

#if ENABLE_ASF
void *close_fd_thread(void *t) {
	int i;
	int fd;
	fd_t * fd_data;
	fd_data = (fd_t *) t;

	fd_data->res = close_fd(fd_data->fd);

	pthread_exit(NULL);
}


int create_thread_close_fd(int fd) {
	int res = 0;
	int rc;
	int i;
	pthread_t thread;
	pthread_attr_t attr;
	void *status;

	// Initialize and set thread joinable
	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

	//void * param = (void *) fd;
	fd_t * fd_data = (struct fd_t*) malloc(sizeof(struct fd_t));
	fd_data->fd = fd;
	void * param = (void *) fd_data;
	rc = pthread_create(&thread, NULL, close_fd_thread, param);
	if (rc) {
		LOGE("Could not create thread for close fd: %d, %d", fd, rc);
		exit(-1);
	}

	// free attribute and wait for the other threads
	pthread_attr_destroy(&attr);
	rc = pthread_join(thread, &status);
	if (rc) {
		LOGE("Could not join: %d, %d", fd, rc);
		exit(-1);
	}

	res = fd_data->res;
	free(fd_data);
	return res;

}

void *get_fd_thread(void *t) {
	int i;
	char * filepath;
	fd_t * fd_data;
	fd_data = (fd_t *) t;

	fd_data->fd = get_fd(fd_data->filepath);
	pthread_exit(NULL);
}

int create_thread_get_fd(const char * filepath) {
	int fd = 0;
	int rc;
	int i;
	pthread_t thread;
	pthread_attr_t attr;
	void *status;

	// Initialize and set thread joinable
	pthread_attr_init(&attr);
	pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_JOINABLE);

	//void * param = (void *) fd;
	fd_t * fd_data = (struct fd_t*) malloc(sizeof(struct fd_t));
	fd_data->filepath = filepath;
	void * param = (void *) fd_data;
	rc = pthread_create(&thread, NULL, get_fd_thread, param);
	if (rc) {
		LOGE("Could not create thread for closing fd: %d, %d", fd, rc);
		exit(-1);
	}

	// free attribute and wait for the other threads
	pthread_attr_destroy(&attr);
	rc = pthread_join(thread, &status);
	if (rc) {
		LOGE("Could not join: %s, %d, %d", filepath, fd, rc);
		exit(-1);
	}

	fd = fd_data->fd;
	free(fd_data);
	return fd;
}
#endif

int lockf(int fd, int cmd, off_t len){
    return 0;
}