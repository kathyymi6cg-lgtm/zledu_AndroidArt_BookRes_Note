# 第12章 Bitmap的加载和Cache

本章的主题是Bitmap的加载和Cache，主要包含三个方面的内容。首先讲述如何有效地加载一个Bitmap，这是一个很有意义的话题，由于Bitmap的特殊性以及Android对单个应用所施加的内存限制，比如16MB，这导致加载Bitmap的时候很容易出现内存溢出。下面这个异常信息在开发中应该时常遇到：

```
 Java. lang.OutOfMemoryError: bitmap size exceeds VM budget
```

因此如何高效地加载Bitmap是一个很重要也很容易被开发者忽视的问题。

接着介绍Android中常用的缓存策略，缓存策略是一个通用的思想，可以用在很多场景中，但是实际开发中经常需要用Bitmap做缓存。通过缓存策略，我们不需要每次都从网络上请求图片或者从存储设备中加载图片，这样就极大地提高了图片的加载效率以及产品的用户体验。目前比较常用的缓存策略是LruCache和DiskLruCache，其中LruCache常被用做内存缓存，而DiskLruCache常被用做存储缓存。Lru是Least Recently Used的缩写，即最近最少使用算法，这种算法的核心思想为：当缓存快满时，会淘汰近期最少使用的缓存目标，很显然Lru算法的思想是很容易被接受的。

最后本章会介绍如何优化列表的卡顿现象，ListView和GridView由于要加载大量的子视图，当用户快速滑动时就容易出现卡顿的现象，因此本章最后针对这个问题将会给出一些优化建议。

为了更好地介绍上述三个主题，本章提供了一个示例程序，该程序会尝试从网络加载大量图片并在GridView中显示，可以发现这个程序具有很强的实用性，并且其技术细节完全覆盖了本章的三个主题：图片加载、缓存策略、列表的滑动流畅性，通过这个示例程序读者可以很好地理解本章的全部内容并能够在实际中灵活应用。

## 12.1 Bitmap的高效加载

在介绍Bitmap的高效加载之前，先说一下如何加载一个Bitmap，Bitmap在Android中指的是一张图片，可以是png格式也可以是jpg等其他常见的图片格式。那么如何加载一个图片呢？BitmapFactory类提供了四类方法：decodeFile、decodeResource、decodeStream和decodeByteArray，分别用于支持从文件系统、资源、输入流以及字节数组中加载出一个Bitmap对象，其中decodeFile和decodeResource又间接调用了decodeStream方法，这四类方法最终是在Android的底层实现的，对应着BitmapFactory类的几个native方法。

如何高效地加载Bitmap呢？其实核心思想也很简单，那就是采用BitmapFactory.Options来加载所需尺寸的图片。这里假设通过ImageView来显示图片，很多时候ImageView并没有图片的原始尺寸那么大，这个时候把整个图片加载进来后再设给ImageView，这显然是没必要的，因为ImageView并没有办法显示原始的图片。通过BitmapFactory.Options就可以按一定的采样率来加载缩小后的图片，将缩小后的图片在ImageView中显示，这样就会降低内存占用从而在一定程度上避免OOM，提高了Bitmap加载时的性能。BitmapFactory提供的加载图片的四类方法都支持BitmapFactory.Options参数，通过它们就可以很方便地对一个图片进行采样缩放。

通过BitmapFactory.Options来缩放图片，主要是用到了它的inSampleSize参数，即采样率。当inSampleSize为1时，采样后的图片大小为图片的原始大小；当inSampleSize大于1时，比如为2，那么采样后的图片其宽/高均为原图大小的1/2，而像素数为原图的1/4，其占有的内存大小也为原图的1/4。拿一张1024x1024像素的图片来说，假定采用ARGB8888格式存储，那么它占有的内存为1024x1024x4，即4MB，如果inSampleSize为2，那么采样后的图片其内存占用只有512x512x4，即1MB。可以发现采样率inSampleSize必须是大于1的整数图片才会有缩小的效果，并且采样率同时作用于宽/高，这将导致缩放后的图片大小以采样率的2次方形式递减，即缩放比例为1/（inSampleSize的2次方），比如inSampleSize为4，那么缩放比例就是1/16。有一种特殊情况，那就是当inSampleSize小于1时，其作用相当于1，即无缩放效果。另外最新的官方文档中指出，inSampleSize的取值应该总是为2的指数，比如1、2、4、8、16，等等。如果外界传递给系统的inSampleSize不为2的指数，那么系统会向下取整并选择一个最接近的2的指数来代替，比如3，系统会选择2来代替，但是经过验证发现这个结论并非在所有的Android版本上都成立，因此把它当成一个开发建议即可。

考虑以下实际的情况，比如ImageView的大小是100x100像素，而图片的原始大小为200x200，那么只需将采样率inSampleSize设为2即可。但是如果图片大小为200x300呢？这个时候采样率还应该选择2，这样缩放后的图片大小为100×150像素，仍然是适合ImageView的，如果采样率为3，那么缩放后的图片大小就会小于ImageView所期望的大小，这样图片就会被拉伸从而导致模糊。

通过采样率即可有效地加载图片，那么到底如何获取采样率呢？获取采样率也很简单，遵循如下流程：

（1）将BitmapFactory.Options的inJustDecodeBounds参数设为true并加载图片。

（2）从BitmapFactory.Options中取出图片的原始宽高信息，它们对应于outWidth和outHeight参数。

（3）根据采样率的规则并结合目标View的所需大小计算出采样率inSampleSize。

（4）将BitmapFactory.Options的inJustDecodeBounds参数设为false，然后重新加载图片。

经过上面4个步骤，加载出的图片就是最终缩放后的图片，当然也有可能不需要缩放。这里说明一下inJustDecodeBounds参数，当此参数设为true时，BitmapFactory只会解析图片的原始宽/高信息，并不会去真正地加载图片，所以这个操作是轻量级的。另外需要注意的是，这个时候BitmapFactory获取的图片宽/高信息和图片的位置以及程序运行的设备有关，比如同一张图片放在不同的drawable目录下或者程序运行在不同屏幕密度的设备上，这都可能导致BitmapFactory获取到不同的结果，之所以会出现这个现象，这和Android的资源加载机制有关，相信读者平日里肯定有所体会，这里就不再详细说明了。

将上面的4个流程用程序来实现，就产生了下面的代码：

```
public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
        int reqWidth, int reqHeight) {
    // First decode with inJustDecodeBounds=true to check dimensions
    final BitmapFactory.Options options = new BitmapFactory.Options();
    options. inJustDecodeBounds = true;
    BitmapFactory.decodeResource(res, resId, options);
    // Calculate inSampleSize
    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight) ;
    // Decode bitmap with inSampleSize set
    options.inJustDecodeBounds = false;
    return BitmapFactory.decodeResource(res, resId, options);
 }
public static int calculateInSampleSize(
           BitmapFactory.Options options, int reqWidth, int reqHeight) {
    // Raw height and width of image
    final int height = options.outHeight;
    final int width = options.outWidth;
    int inSampleSize = 1;
    if(height > reqHeight || width > reqWidth) {
        final int halfHeight = height / 2;
        final int halfWidth = width / 2;
        // Calculate the largest inSampleSize value that is a power of 2 and
          keeps both
        // height and width larger than the requested height and width.
       while((halfHeight / inSampleSize) >= reqHeight
              && (halfWidth / inSampleSize) >= reqWidth) {
           inSampleSize *= 2;
        }
    }
    return inSampleSize;
 }
```

有了上面的两个方法，实际使用的时候就很简单了，比如ImageView所期望的图片大小为100×100像素，这个时候就可以通过如下方式高效地加载并显示图片：

```
mImageView.setImageBitmap(
    decodeSampledBitmapFromResource(getResources(), R.id.myimage, 100, 100));
```

除了BitmapFactory的decodeResource方法，其他三个decode系列的方法也是支持采样加载的，并且处理方式也是类似的，但是decodeStream方法稍微有点特殊，这个会在后续内容中详细介绍。通过本节的介绍，读者应该能很好地掌握这种高效地加载图片的方法了。

## 12.2 Android中的缓存策略

缓存策略在Android中有着广泛的使用场景，尤其在图片加载这个场景下，缓存策略就变得更为重要。考虑一种场景：有一批网络图片，需要下载后在用户界面上予以显示，这个场景在PC环境下是很简单的，直接把所有的图片下载到本地再显示即可，但是放到移动设备上就不一样了。不管是Android还是iOS设备，流量对于用户来说都是一种宝贵的资源，由于流量是收费的，所以在应用开发中并不能过多地消耗用户的流量，否则这个应用肯定不能被用户所接受。再加上目前国内公共场所的WiFi普及率并不算太高，因此用户在很多情况下手机上都是用的移动网络而非WiFi，因此必须提供一种解决方案来解决流量的消耗问题。

如何避免过多的流量消耗呢？那就是本节所要讨论的主题：缓存。当程序第一次从网络加载图片后，就将其缓存到存储设备上，这样下次使用这张图片就不用再从网络上获取了，这样就为用户节省了流量。很多时候为了提高应用的用户体验，往往还会把图片在内存中再缓存一份，这样当应用打算从网络上请求一张图片时，程序会首先从内存中去获取，如果内存中没有那就从存储设备中去获取，如果存储设备中也没有，那就从网络上下载这张图片。因为从内存中加载图片比从存储设备中加载图片要快，所以这样既提高了程序的效率又为用户节约了不必要的流量开销。上述的缓存策略不仅仅适用于图片，也适用于其他文件类型。

说到缓存策略，其实并没有统一的标准。一般来说，缓存策略主要包含缓存的添加、获取和删除这三类操作。如何添加和获取缓存这个比较好理解，那么为什么还要删除缓存呢？这是因为不管是内存缓存还是存储设备缓存，它们的缓存大小都是有限制的，因为内存和诸如SD卡之类的存储设备都是有容量限制的，因此在使用缓存时总是要为缓存指定一个最大的容量。如果当缓存容量满了，但是程序还需要向其添加缓存，这个时候该怎么办呢？这就需要删除一些旧的缓存并添加新的缓存，如何定义缓存的新旧这就是一种策略，不同的策略就对应着不同的缓存算法，比如可以简单地根据文件的最后修改时间来定义缓存的新旧，当缓存满时就将最后修改时间较早的缓存移除，这就是一种缓存算法，但是这种算法并不算很完美。

目前常用的一种缓存算法是LRU（Least Recently Used），LRU是近期最少使用算法，它的核心思想是当缓存满时，会优先淘汰那些近期最少使用的缓存对象。采用LRU算法的缓存有两种：LruCache和DiskLruCache，LruCache用于实现内存缓存，而DiskLruCache则充当了存储设备缓存，通过这二者的完美结合，就可以很方便地实现一个具有很高实用价值的ImageLoader。本节首先会介绍LruCache和DiskLruCache，然后利用LruCache和DiskLruCache来实现一个优秀的ImageLoader，并且提供一个使用ImageLoader来从网络下载并展示图片的例子，在这个例子中体现了ImageLoader以及大批量网络图片加载所涉及的大量技术点。

### 12.2.1 LruCache

LruCache是Android3.1所提供的一个缓存类，通过support-v4兼容包可以兼容到早期的Android版本，目前Android2.2以下的用户量已经很少了，因此我们开发的应用兼容到Android2.2就已经足够了。为了能够兼容Android2.2版本，在使用LruCache时建议采用support-v4兼容包中提供的LruCache，而不要直接使用Android3.1提供的LruCache。

LruCache是一个泛型类，它内部采用一个LinkedHashMap以强引用的方式存储外界的缓存对象，其提供了get和put方法来完成缓存的获取和添加操作，当缓存满时，LruCache会移除较早使用的缓存对象，然后再添加新的缓存对象。这里读者要明白强引用、软引用和弱引用的区别，如下所示。

强引用：直接的对象引用；

软引用：当一个对象只有软引用存在时，系统内存不足时此对象会被gc回收；

弱引用：当一个对象只有弱引用存在时，此对象会随时被gc回收。

另外LruCache是线程安全的，下面是LruCache的定义：

```
public class LruCache<K, V> {
    private final LinkedHashMap<K, V> map;
    ...
}
```

LruCache的实现比较简单，读者可以参考它的源码，这里仅介绍如何使用LruCache来实现内存缓存。仍然拿图片缓存来举例子，下面的代码展示了LruCache的典型的初始化过程：

```
 int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
 int cacheSize = maxMemory / 8;
mMemoryCache = new LruCache<String, Bitmap> (cacheSize) {
     @Override
    protected int sizeOf(String key, Bitmap bitmap) {
         return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
     }
 };
```

在上面的代码中，只需要提供缓存的总容量大小并重写sizeOf方法即可。sizeOf方法的作用是计算缓存对象的大小，这里大小的单位需要和总容量的单位一致。对于上面的示例代码来说，总容量的大小为当前进程的可用内存的1/8，单位为KB，而sizeOf方法则完成了Bitmap对象的大小计算。很明显，之所以除以1024也是为了将其单位转换为KB。一些特殊情况下，还需要重写LruCache的entryRemoved方法，LruCache移除旧缓存时会调用entryRemoved方法，因此可以在entryRemoved中完成一些资源回收工作（如果需要的话）。

除了LruCache的创建以外，还有缓存的获取和添加，这也很简单，从LruCache中获取一个缓存对象，如下所示。

```
mMemoryCache.get(key)
```

向LruCache中添加一个缓存对象，如下所示。

```
mMemoryCache.put(key, bitmap)
```

LruCache还支持删除操作，通过remove方法即可删除一个指定的缓存对象。可以看到LruCache的实现以及使用都非常简单，虽然简单，但是仍然不影响它具有强大的功能，从Android3.1开始，LruCache就已经是Android源码的一部分了。

### 12.2.2 DiskLruCache

DiskLruCache用于实现存储设备缓存，即磁盘缓存，它通过将缓存对象写入文件系统从而实现缓存的效果。DiskLruCache得到了Android官方文档的推荐，但它不属于Android SDK的一部分，它的源码可以从如下网址得到：

```
https://android.googlesource.com/platform/libcore/+/android-4.1.1_r1/luni/src/main/java/libcore/io/DiskLruCache.java
```

需要注意的是，从上述网址获取的DiskLruCache的源码并不能直接在Android中使用，需要稍微修改编译错误。下面分别从DiskLruCache的创建、缓存查找和缓存添加这三个方面来介绍DiskLruCache的使用方式。

1.DiskLruCache的创建

DiskLruCache并不能通过构造方法来创建，它提供了open方法用于创建自身，如下所示。

```
 public static DiskLruCache open(File directory, int appVersion, int
 valueCount, long maxSize)
```

open方法有四个参数，其中第一个参数表示磁盘缓存在文件系统中的存储路径。缓存路径可以选择SD卡上的缓存目录，具体是指/sdcard/Android/data/package_name/cache目录，其中package_name表示当前应用的包名，当应用被卸载后，此目录会一并被删除。当然也可以选择SD卡上的其他指定目录，还可以选择data下的当前应用的目录，具体可根据需要灵活设定。这里给出一个建议：如果应用卸载后就希望删除缓存文件，那么就选择SD卡上的缓存目录，如果希望保留缓存数据那就应该选择SD卡上的其他特定目录。

第二个参数表示应用的版本号，一般设为1即可。当版本号发生改变时DiskLruCache会清空之前所有的缓存文件，而这个特性在实际开发中作用并不大，很多情况下即使应用的版本号发生了改变缓存文件却仍然是有效的，因此这个参数设为1比较好。

第三个参数表示单个节点所对应的数据的个数，一般设为1即可。第四个参数表示缓存的总大小，比如50MB，当缓存大小超出这个设定值后，DiskLruCache会清除一些缓存从而保证总大小不大于这个设定值。下面是一个典型的DiskLruCache的创建过程：

```
private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50; //50MB
 File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
 if(!diskCacheDir.exists()) {
     diskCacheDir.mkdirs();
 }
mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1, DISK_CACHE_SIZE);
```

2.DiskLruCache的缓存添加

DiskLruCache的缓存添加的操作是通过Editor完成的，Editor表示一个缓存对象的编辑对象。这里仍然以图片缓存举例，首先需要获取图片url所对应的key，然后根据key就可以通过edit()来获取Editor对象，如果这个缓存正在被编辑，那么edit()会返回null，即DiskLruCache不允许同时编辑一个缓存对象。之所以要把url转换成key，是因为图片的url中很可能有特殊字符，这将影响url在Android中直接使用，一般采用url的md5值作为key，如下所示。

```
private String hashKeyFormUrl(String url) {
     String cacheKey;
     try {
         final MessageDigest mDigest = MessageDigest.getInstance("MD5") ;
         mDigest.update(url.getBytes());
         cacheKey = bytesToHexString(mDigest.digest());
     } catch(NoSuchAlgorithmException e) {
         cacheKey = String.valueOf(url.hashCode());
     }
     return cacheKey;
 }
private String bytesToHexString(byte[] bytes) {
     StringBuilder sb = new StringBuilder();
     for(int i = 0; i < bytes.length; i++) {
         String hex = Integer.toHexString(0xFF & bytes[i]);
         if(hex.length() == 1) {
             sb.append('0') ;
         }
         sb.append(hex) ;
     }
     return sb.toString();
 }
```

将图片的url转成key以后，就可以获取Editor对象了。对于这个key来说，如果当前不存在其他Editor对象，那么edit()就会返回一个新的Editor对象，通过它就可以得到一个文件输出流。需要注意的是，由于前面在DiskLruCache的open方法中设置了一个节点只能有一个数据，因此下面的DISK_CACHE_INDEX常量直接设为0即可，如下所示。

```
 String key = hashKeyFormUrl(url) ;
 DiskLruCache.Editor editor = mDiskLruCache.edit(key);
 if(editor != null) {
    OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX) ;
 }
```

有了文件输出流，接下来要怎么做呢？其实是这样的，当从网络下载图片时，图片就可以通过这个文件输出流写入到文件系统上，这个过程的实现如下所示。

```
public boolean downloadUrlToStream(String urlString,
         OutputStream outputStream) {
     HttpURLConnection urlConnection = null;
    BufferedOutputStream out = null;
     BufferedInputStream in = null;
     try {
         final URL url = new URL(urlString);
         urlConnection = (HttpURLConnection) url.openConnection();
         in = new BufferedInputStream(urlConnection.getInputStream(),
                 IO_BUFFER_SIZE);
         out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
         int b;
         while((b = in.read()) != -1) {
             out.write(b);
         }
         return true;
     } catch(IOException e) {
         Log.e(TAG, "downloadBitmap failed." + e);
     } finally {
         if(urlConnection != null) {
             urlConnection.disconnect();
         }
         MyUtils.close(out) ;
         MyUtils.close(in);
     }
     return false;
 }
```

经过上面的步骤，其实并没有真正地将图片写入文件系统，还必须通过Editor的commit()来提交写入操作，如果图片下载过程发生了异常，那么还可以通过Editor的abort()来回退整个操作，这个过程如下所示。

```
 OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX) ;
 if(downloadUrlToStream(url, outputStream)) {
     editor.commit();
 } else {
     editor.abort();
 }
 mDiskLruCache.flush();
```

经过上面的几个步骤，图片已经被正确地写入到文件系统了，接下来图片获取的操作就不需要请求网络了。

3.DiskLruCache的缓存查找

和缓存的添加过程类似，缓存查找过程也需要将url转换为key，然后通过DiskLruCache的get方法得到一个Snapshot对象，接着再通过Snapshot对象即可得到缓存的文件输入流，有了文件输出流，自然就可以得到Bitmap对象了。为了避免加载图片过程中导致的OOM问题，一般不建议直接加载原始图片。在第12.1节中已经介绍了通过BitmapFactory.Options对象来加载一张缩放后的图片，但是那种方法对FileInputStream的缩放存在问题，原因是FileInputStream是一种有序的文件流，而两次decodeStream调用影响了文件流的位置属性，导致了第二次decodeStream时得到的是null。为了解决这个问题，可以通过文件流来得到它所对应的文件描述符，然后再通过BitmapFactory.decodeFileDescriptor方法来加载一张缩放后的图片，这个过程的实现如下所示。

```
 Bitmap bitmap = null;
 String key = hashKeyFormUrl(url) ;
 DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
 if(snapShot != null) {
     FileInputStream fileInputStream = (FileInputStream)snapShot.getInputStream(DISK_CACHE_INDEX) ;
     FileDescriptor fileDescriptor = fileInputStream.getFD();
     bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor
     (fileDescriptor,
             reqWidth, reqHeight);
     if(bitmap != null) {
         addBitmapToMemoryCache(key, bitmap);
     }
 }
```

上面介绍了DiskLruCache的创建、缓存的添加和查找过程，读者应该对DiskLruCache的使用方式有了一个大致的了解，除此之外，DiskLruCache还提供了remove、delete等方法用于磁盘缓存的删除操作。关于DiskLruCache的内部实现这里就不再介绍了，读者感兴趣的话可以查看它的源码实现。

### 12.2.3 ImageLoader的实现

在本章的前面先后介绍了Bitmap的高效加载方式、LruCache以及DiskLruCache，现在我们来着手实现一个优秀的ImageLoader。

一般来说，一个优秀的ImageLoader应该具备如下功能：

图片的同步加载；

图片的异步加载；

图片压缩：

内存缓存；

磁盘缓存；

网络拉取。

图片的同步加载是指能够以同步的方式向调用者提供所加载的图片，这个图片可能是从内存缓存中读取的，也可能是从磁盘缓存中读取的，还可能是从网络拉取的。图片的异步加载是一个很有用的功能，很多时候调用者不想在单独的线程中以同步的方式来获取图片，这个时候ImageLoader内部需要自己在线程中加载图片并将图片设置给所需的ImageView。图片压缩的作用更毋庸置疑了，这是降低OOM概率的有效手段，ImageLoader必须合适地处理图片的压缩问题。

内存缓存和磁盘缓存是ImageLoader的核心，也是ImageLoader的意义之所在，通过这两级缓存极大地提高了程序的效率并且有效地降低了对用户所造成的流量消耗，只有当这两级缓存都不可用时才需要从网络中拉取图片。

除此之外，ImageLoader还需要处理一些特殊的情况，比如在ListView或者GridView中，View复用既是它们的优点也是它们的缺点，优点想必读者都很清楚了，那缺点可能还不太清楚。考虑一种情况，在ListView或者GridView中，假设一个itemA正在从网络加载图片，它对应的ImageView为A，这个时候用户快速向下滑动列表，很可能itemB复用了ImageViewA，然后等了一会之前的图片下载完毕了。如果直接给ImageViewA设置图片，由于这个时候ImageViewA被itemB所复用，但是itemB要显示的图片显然不是item A刚刚下载好的图片，这个时候就会出现itemB中显示了itemA的图片，这就是常见的列表的错位问题，ImageLoader需要正确地处理这些特殊情况。

上面对ImageLoader的功能做了一个全面的分析，下面就可以一步步实现一个ImageLoader了，这里主要分为如下几步。

1.图片压缩功能的实现

图片压缩在第12.1节中已经做了介绍，这里就不再多说了，为了有良好的设计风格，这里单独抽象了一个类用于完成图片的压缩功能，这个类叫ImageResizer，它的实现如下所示。

```
 public class ImageResizer {
    private static final String TAG = "ImageResizer";
    public ImageResizer() {
    }
    public Bitmap decodeSampledBitmapFromResource(Resources res,
           int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options. inJustDecodeBounds = true;
       BitmapFactory.decodeResource(res, resId, options);
        // Calculate inSampleSize
       options.inSampleSize = calculateInSampleSize(options, reqWidth,
              reqHeight) ;
        // Decode bitmap with inSampleSize set
       options. inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
    public Bitmap decodeSampledBitmapFromFileDescriptor(FileDescriptor fd,
    int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
       options. inJustDecodeBounds = true;
       BitmapFactory.decodeFileDescriptor(fd, null, options);
        // Calculate inSampleSize
       options.inSampleSize = calculateInSampleSize(options, reqWidth,
              reqHeight) ;
        // Decode bitmap with inSampleSize set
       options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFileDescriptor(fd, null, options);
    }
    public int calculateInSampleSize(BitmapFactory.Options options,
           int reqWidth, int reqHeight) {
        if(reqWidth == 0 || reqHeight == 0) {
           return 1;
        }
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
       Log.d(TAG, "origin, w=" + width + " h=" + height);
        int inSampleSize = 1;
        if(height > reqHeight || width > reqWidth) {
           final int halfHeight = height / 2;
           final int halfWidth = width / 2;
           // Calculate the largest inSampleSize value that is a power of 2 and
           // keeps both
           // height and width larger than the requested height and width.
           while((halfHeight / inSampleSize) >= reqHeight
                  && (halfWidth / inSampleSize) >= reqWidth) {
               inSampleSize *= 2;
           }
        }
        Log.d(TAG, "sampleSize:" + inSampleSize);
        return inSampleSize;
    }
 }
```

2.内存缓存和磁盘缓存的实现

这里选择LruCache和DiskLruCache来分别完成内存缓存和磁盘缓存的工作。在ImageLoader初始化时，会创建LruCache和DiskLruCache，如下所示。

```
 private LruCache<String, Bitmap> mMemoryCache;
private DiskLruCache mDiskLruCache;
private ImageLoader(Context context) {
    mContext = context.getApplicationContext();
     int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
     int cacheSize = maxMemory / 8;
    mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
         @Override
         protected int sizeOf(String key, Bitmap bitmap) {
             return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
         }
     };
    File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
     if(!diskCacheDir.exists()) {
        diskCacheDir.mkdirs();
     }
    if(getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
         try {
            mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1,
                     DISK_CACHE_SIZE);
            mIsDiskLruCacheCreated = true;
         } catch(IOException e) {
             e.printStackTrace();
         }
     }
 }
```

在创建磁盘缓存时，这里做了一个判断，即有可能磁盘剩余空间小于磁盘缓存所需的大小，一般是指用户的手机空间已经不足了，因此没有办法创建磁盘缓存，这个时候磁盘缓存就会失效。在上面的代码实现中，ImageLoader的内存缓存的容量为当前进程可用内存的1/8，磁盘缓存的容量为50MB。

内存缓存和磁盘缓存创建完毕后，还需要提供方法来完成缓存的添加和获取功能。首先看内存缓存，它的添加和读取过程比较简单，如下所示。

```
private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
     if(getBitmapFromMemCache(key) == null) {
         mMemoryCache.put(key, bitmap);
     }
 }
private Bitmap getBitmapFromMemCache(String key) {
     return mMemoryCache.get(key) ;
 }
```

而磁盘缓存的添加和读取功能稍微复杂一些，具体内容已经在 12.2.2 节中进行了详细的介绍，这里再简单说明一下。磁盘缓存的添加需要通过Editor来完成，Editor提供了commit和abort方法来提交和撤销对文件系统的写操作，具体实现请参看下面的loadBitmap FromHttp方法。磁盘缓存的读取需要通过Snapshot来完成，通过Snapshot可以得到磁盘缓存对象对应的FileInputStream，但是FileInputStream无法便捷地进行压缩，所以通过FileDescriptor来加载压缩后的图片，最后将加载后的Bitmap添加到内存缓存中，具体实现请参看下面的loadBitmapFromDiskCache方法。

```
private Bitmap loadBitmapFromHttp(String url, int reqWidth, int reqHeight)
         throws IOException{
     if(Looper.myLooper() == Looper.getMainLooper()) {
         throw new RuntimeException("can not visit network from UI Thread.") ;
     }
     if(mDiskLruCache == null) {
         return null;
     }
     String key = hashKeyFormUrl(url) ;
     DiskLruCache.Editor editor = mDiskLruCache.edit(key);
     if(editor != null) {
         OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX) ;
         if(downloadUrlToStream(url, outputStream)) {
             editor.commit();
         } else {
             editor.abort();
         }
         mDiskLruCache. flush();
     }
     return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
 }
 private Bitmap loadBitmapFromDiskCache(String url, int reqWidth,
         int reqHeight) throws IOException {
     if(Looper.myLooper() == Looper.getMainLooper()) {
         Log.w(TAG, "load bitmap from UI Thread, it's not recommended!");
     }
     if(mDiskLruCache == null) {
         return null;
     }
     Bitmap bitmap = null;
String key = hashKeyFormUrl(url) ;
     DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
     if(snapShot != null) {
         FileInputStream fileInputStream = (FileInputStream)snapShot.
         getInputStream(DISK_CACHE_INDEX) ;
         FileDescriptor fileDescriptor = fileInputStream.getFD();
         bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor
         (fileDescriptor,
                 reqWidth, reqHeight);
         if(bitmap != null) {
             addBitmapToMemoryCache(key, bitmap);
         }
     }
     return bitmap;
 }
```

3.同步加载和异步加载接口的设计

首先看同步加载，同步加载接口需要外部在线程中调用，这是因为同步加载很可能比较耗时，它的实现如下所示。

```
 /**
  * load bitmap from memory cache or disk cache or network.
  * @param uri http url
  * @param reqWidth the width ImageView desired
  * @param reqHeight the height ImageView desired
  * @return bitmap, maybe null.
  */
 public Bitmap loadBitmap(String uri, int reqWidth, int reqHeight) {
     Bitmap bitmap = loadBitmapFromMemCache(uri);
     if(bitmap != null) {
         Log.d(TAG, "loadBitmapFromMemCache,url:" + uri);
         return bitmap;
     }
     try {
         bitmap = loadBitmapFromDiskCache(uri, reqWidth, reqHeight);
         if(bitmap != null) {
             Log.d(TAG, "loadBitmapFromDisk,url:" + uri);
             return bitmap;
         }
         bitmap = loadBitmapFromHttp(uri, reqWidth, reqHeight);
         Log.d(TAG, "loadBitmapFromHttp,url:" + uri);
     } catch(IOException e) {
         e.printStackTrace();
     }
     if(bitmap == null && !mIsDiskLruCacheCreated) {
         Log.w(TAG, "encounter error, DiskLruCache is not created.");
         bitmap = downloadBitmapFromUrl(uri) ;
     }
     return bitmap;
 }
```

从loadBitmap的实现可以看出，其工作过程遵循如下几步：首先尝试从内存缓存中读取图片，接着尝试从磁盘缓存中读取图片，最后才从网络中拉取图片。另外，这个方法不能在主线程中调用，否则就抛出异常。这个执行环境的检查是在loadBitmapFromHttp中实现的，通过检查当前线程的Looper是否为主线程的Looper来判断当前线程是否是主线程，如果不是主线程就直接抛出异常中止程序，如下所示。

```
 if(Looper.myLooper() == Looper.getMainLooper()) {
     throw new RuntimeException("can not visit network from UI Thread.");
 }
```

接着看异步加载接口的设计，如下所示。

```
 public void bindBitmap(final String uri, final ImageView imageView,
         final int reqWidth, final int reqHeight) {
     imageView.setTag(TAG_KEY_URI, uri);
     Bitmap bitmap = loadBitmapFromMemCache(uri);
     if(bitmap != null) {
         imageView.setImageBitmap(bitmap) ;
         return;
     }
    Runnable loadBitmapTask = new Runnable() {
         @Override
        public void run() {
             Bitmap bitmap = loadBitmap(uri, reqWidth, reqHeight);
             if(bitmap != null) {
                 LoaderResult result = new LoaderResult(imageView, uri, bitmap) ;
                 mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).
                 sendToTarget();
             }
         }
     };
    THREAD_POOL_EXECUTOR.execute(loadBitmapTask) ;
 }
```

从bindBitmap的实现来看，bindBitmap方法会尝试从内存缓存中读取图片，如果读取成功就直接返回结果，否则会在线程池中去调用loadBitmap方法，当图片加载成功后再将图片、图片的地址以及需要绑定的imageView封装成一个LoaderResult对象，然后再通过mMainHandler向主线程发送一个消息，这样就可以在主线程中给imageView设置图片了，之所以通过Handler来中转是因为子线程无法访问UI。

bindBitmap中用到了线程池和Handler，这里看一下它们的实现，首先看线程池THREAD_POOL_EXECUTOR的实现，如下所示。可以看出它的核心线程数为当前设备的CPU核心数加1，最大容量为CPU核心数的2倍加1，线程闲置超时时长为10秒，关于线程池的详细介绍可以参看第11章的有关内容。

```
private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
private static final long KEEP_ALIVE = 10L;
private static final ThreadFactory sThreadFactory = new ThreadFactory() {
     private final AtomicInteger mCount = new AtomicInteger(1);
     public Thread newThread(Runnable r) {
         return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
     }
 };
public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
         CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
         KEEP_ALIVE, TimeUnit.SECONDS,
         new LinkedBlockingQueue<Runnable>(), sThreadFactory);
```

之所以采用线程池是有原因的，首先肯定不能采用普通的线程去做这个事，线程池的好处在第11章已经做了详细的说明。如果直接采用普通的线程去加载图片，随着列表的滑动这可能会产生大量的线程，这样并不利于整体效率的提升。另外一点，这里也没有选择采用AsyncTask，AsyncTask封装了线程池和Handler，按道理它应该适合ImageLoader的场景。从第11章中对AsyncTask的分析可以知道，AsyncTask在3.0的低版本和高版本上具有不同的表现，在3.0以上的版本AsyncTask无法实现并发的效果，这显然是不能接受的，因为ImageLoader就是需要并发特性，虽然可以通过改造AsyncTask或者使用AsyncTask的executeOnExecutor方法的形式来执行异步任务，但是这终归是不太自然的实现方式。鉴于以上两点原因，这里选择线程池和Handler来提供ImageLoader的并发能力和访问UI的能力。

分析完线程池的选择，下面看一下Handler的实现，如下所示。ImageLoader直接采用主线程的Looper来构造Handler对象，这就使得ImageLoader可以在非主线程中构造了。另外为了解决由于View复用所导致的列表错位这一问题，在给ImageView设置图片之前都会检查它的url有没有发生改变，如果发生改变就不再给它设置图片，这样就解决了列表错位的问题。

```
private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
     @Override
     public void handleMessage(Message msg) {
         LoaderResult result = (LoaderResult) msg.obj;
         ImageView imageView = result.imageView;
         imageView.setImageBitmap(result.bitmap);
         String uri = (String) imageView.getTag(TAG_KEY_URI);
         if(uri.equals(result.uri)) {
             imageView.setImageBitmap(result.bitmap) ;
         } else {
             Log.w(TAG, "set image bitmap,but url has changed, ignored!");
         }
     }
 };
```

到此为止，ImageLoader的细节都已经做了全面的分析，下面是ImageLoader的完整的代码。

```
 public class ImageLoader{
    private static final String TAG = "ImageLoader";
    public static final int MESSAGE_POST_RESULT = 1;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
    private static final long KEEP_ALIVE = 10L;
    private static final int TAG_KEY_URI = R.id.imageloader_uri;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 50;
    private static final int IO_BUFFER_SIZE = 8 * 1024;
    private static final int DISK_CACHE_INDEX = 0;
    private boolean mIsDiskLruCacheCreated = false;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
       private final AtomicInteger mCount = new AtomicInteger(1);
       public Thread newThread(Runnable r) {
           return new Thread(r, "ImageLoader#" + mCount.getAndIncrement());
        }
    };
    public static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
           CORE_POOL_SIZE, MAXIMUM_POOL_SIZE,
           KEEP_ALIVE, TimeUnit.SECONDS,
           new LinkedBlockingQueue<Runnable>(), sThreadFactory);
    private Handler mMainHandler = new Handler(Looper.getMainLooper()) {
       @Override
       public void handleMessage(Message msg) {
           LoaderResult result = (LoaderResult) msg.obj;
           ImageView imageView = result.imageView;
           imageView.setImageBitmap(result.bitmap);
           String uri = (String) imageView.getTag(TAG_KEY_URI);
           if(uri.equals(result.uri)) {
       imageView.setImageBitmap(result.bitmap) ;
           } else {
              Log.w(TAG, "set image bitmap,but url has changed, ignored!");
      }
        }
    };
    private Context mContext;
    private ImageResizer mImageResizer = new ImageResizer();
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    private ImageLoader(Context context) {
       mContext = context.getApplicationContext();
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
       mMemoryCache = new LruCache<String, Bitmap> (cacheSize) {
           @Override
           protected int sizeOf(String key, Bitmap bitmap) {
               return bitmap.getRowBytes() * bitmap.getHeight() / 1024;
           }
        };
        File diskCacheDir = getDiskCacheDir(mContext, "bitmap");
        if(!diskCacheDir.exists()) {
           diskCacheDir.mkdirs();
        }
        if(getUsableSpace(diskCacheDir) > DISK_CACHE_SIZE) {
           try {
              mDiskLruCache = DiskLruCache.open(diskCacheDir, 1, 1,
                      DISK_CACHE_SIZE);
               mIsDiskLruCacheCreated = true;
            } catch(IOException e) {
               e.printStackTrace();
           }
        }
    }
    /**
     * build a new instance of ImageLoader
     * @param context
     * @return a new instance of ImageLoader
     */
    public static ImageLoader build(Context context) {
        return new ImageLoader(context) ;
    }
    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if(getBitmapFromMemCache(key) == null) {
           mMemoryCache.put(key, bitmap);
        }
    }
    private Bitmap getBitmapFromMemCache(String key) {
       return mMemoryCache.get(key) ;
    }
    /**
     * load bitmap from memory cache or disk cache or network async, then bind
     imageView and bitmap.
     * NOTE THAT: should run in UI Thread
     * @param uri http url
     * @param imageView bitmap's bind object
     */
    public void bindBitmap(final String uri, final ImageView imageView) {
       bindBitmap(uri, imageView, 0, 0);
    }
    public void bindBitmap(final String uri, final ImageView imageView,
           final int reqWidth, final int reqHeight) {
       imageView.setTag(TAG_KEY_URI, uri);
       Bitmap bitmap = loadBitmapFromMemCache(uri);
       if(bitmap != null) {
           imageView.setImageBitmap(bitmap) ;
           return;
        }
       Runnable loadBitmapTask = new Runnable() {
           @Override
           public void run() {
              Bitmap bitmap = loadBitmap(uri, reqWidth, reqHeight);
              if(bitmap != null) {
                  LoaderResult result = new LoaderResult(imageView, uri,
                  bitmap) ;
                  mMainHandler.obtainMessage(MESSAGE_POST_RESULT, result).
                  sendToTarget();
               }
           }
        };
       THREAD_POOL_EXECUTOR.execute(loadBitmapTask) ;
    }
    /**
     * load bitmap from memory cache or disk cache or network.
     * @param uri http url
     * @param reqWidth the width ImageView desired
     * @param reqHeight the height ImageView desired
     * @return bitmap, maybe null.
     */
    public Bitmap loadBitmap(String uri, int reqWidth, int reqHeight) {
        Bitmap bitmap = loadBitmapFromMemCache(uri);
        if(bitmap != null) {
           Log.d(TAG, "loadBitmapFromMemCache,url:" + uri);
           return bitmap;
        }
        try {
           bitmap = loadBitmapFromDiskCache(uri, reqWidth, reqHeight);
           if(bitmap != null) {
               Log.d(TAG, "loadBitmapFromDisk,url:" + uri);
               return bitmap;
           }
           bitmap = loadBitmapFromHttp(uri, reqWidth, reqHeight);
           Log.d(TAG, "loadBitmapFromHttp,url:" + uri);
        } catch(IOException e) {
           e.printStackTrace();
        }
        if(bitmap == null && !mIsDiskLruCacheCreated) {
           Log.w(TAG, "encounter error, DiskLruCache is not created.");
           bitmap = downloadBitmapFromUrl(uri);
        }
        return bitmap;
    }
    private Bitmap loadBitmapFromMemCache(String url) {
        final String key = hashKeyFormUrl(url);
       Bitmap bitmap = getBitmapFromMemCache(key) ;
       return bitmap;
    }
    private Bitmap loadBitmapFromHttp(String url, int reqWidth, int
    reqHeight)
           throws IOException{
       if(Looper.myLooper() == Looper.getMainLooper()) {
           throw new RuntimeException("can not visit network from UI Thread.");
        }
       if(mDiskLruCache == null) {
           return null;
        }
       String key = hashKeyFormUrl(url) ;
       DiskLruCache.Editor editor = mDiskLruCache.edit(key) ;
       if(editor != null) {
           OutputStream outputStream = editor.newOutputStream(DISK_CACHE_INDEX) ;
           if(downloadUrlToStream(url, outputStream)) {
              editor.commit();
           } else {
              editor.abort();
           }
           mDiskLruCache.flush();
        }
        return loadBitmapFromDiskCache(url, reqWidth, reqHeight);
    }
    private Bitmap loadBitmapFromDiskCache(String url, int reqWidth,
           int reqHeight) throws IOException{
        if(Looper.myLooper() == Looper.getMainLooper()) {
           Log.w(TAG, "load bitmap from UI Thread, it's not recommended!") ;
        }
        if(mDiskLruCache == null) {
           return null;
        }
       Bitmap bitmap = null;
        String key = hashKeyFormUrl(url);
        DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key) ;
    if (snapShot != null) {
           FileInputStream fileInputStream = (FileInputStream)snapShot.
           getInputStream(DISK_CACHE_INDEX) ;
           FileDescriptor fileDescriptor = fileInputStream.getFD();
           bitmap = mImageResizer.decodeSampledBitmapFromFileDescriptor
           (fileDescriptor, reqWidth, reqHeight);
           if(bitmap != null) {
               addBitmapToMemoryCache(key, bitmap);
           }
        }
        return bitmap;
    }
    public boolean downloadUrlToStream(String urlString,
           OutputStream outputStream) {
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;
        BufferedInputStream in = null;
       try {
          final URL url = new URL(urlString);
          urlConnection = (HttpURLConnection) url.openConnection();
          in = new BufferedInputStream(urlConnection.getInputStream(),
                  IO_BUFFER_SIZE);
          out = new BufferedOutputStream(outputStream, IO_BUFFER_SIZE);
           int b;
          while((b = in.read()) != -1) {
              out.write(b) ;
           }
           return true;
       } catch(IOException e) {
           Log.e(TAG, "downloadBitmap failed." + e);
       } finally{
           if(urlConnection != null) {
              urlConnection.disconnect();
           }
           MyUtils.close(out);
           MyUtils.close(in);
        }
       return false;
    }
    private Bitmap downloadBitmapFromUrl(String urlString) {
        Bitmap bitmap = null;
        HttpURLConnection urlConnection = null;
        BufferedInputStream in = null;
        try {
           final URL url = new URL(urlString);
           urlConnection = (HttpURLConnection) url.openConnection();
           in = new BufferedInputStream(urlConnection.getInputStream(),
                  IO_BUFFER_SIZE) ;
           bitmap = BitmapFactory.decodeStream(in);
        } catch(final IOException e) {
           Log.e(TAG, "Error in downloadBitmap: " + e);
        } finally {
            if(urlConnection != null) {
               urlConnection.disconnect();
           }
           MyUtils.close(in);
        }
        return bitmap;
    }
    private String hashKeyFormUrl(String url) {
       String cacheKey;
        try {
           final MessageDigest mDigest = MessageDigest.getInstance("MD5");
           mDigest.update(url.getBytes());
           cacheKey = bytesToHexString(mDigest.digest());
        } catch(NoSuchAlgorithmException e) {
           cacheKey = String.valueOf(url.hashCode());
        }
        return cacheKey;
    }
    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < bytes.length; i++) {
           String hex = Integer.toHexString(0xFF & bytes[i]);
           if(hex.length() == 1) {
               sb.append('0') ;
           }
           sb.append(hex) ;
        }
        return sb.toString();
    }
    public File getDiskCacheDir(Context context, String uniqueName) {
       boolean externalStorageAvailable = Environment
               .getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ;
        final String cachePath;
        if(externalStorageAvailable) {
           cachePath = context.getExternalCacheDir().getPath();
        } else {
           cachePath = context.getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName) ;
    }
    @TargetApi(VERSION_CODES.GINGERBREAD)
    private long getUsableSpace(File path) {
        if(Build.VERSION. SDK_INT >= VERSION_CODES.GINGERBREAD) {
           return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return(long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }
    private static class LoaderResult {
       public ImageView imageView;
       public String uri;
       public Bitmap bitmap;
       public LoaderResult(ImageView imageView, String uri, Bitmap bitmap) {
           this.imageView = imageView;
           this.uri = uri;
           this.bitmap = bitmap;
        }
    }
 }
```

## 12.3 ImageLoader的使用

在12.2.3节中我们实现了一个功能完整的ImageLoader，本节将演示如何通过Image Loader来实现一个照片墙的效果，实际上我们会发现，通过ImageLoader打造一个照片墙是轻而易举的事情。最后针对如何提高列表的滑动流畅度这个问题，本节会给出一些针对性的建议供读者参考。

### 12.3.1 照片墙效果

实现照片墙效果需要用到GridView，下面先准备好GridView所需的布局文件以及item的布局文件，如下所示。

```
 //GridView的布局文件
 <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="5dp" >
    <GridView
        android:id="@+id/gridView1"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:horizontalSpacing="5dp"
        android:verticalSpacing="5dp"
        android:listSelector="@android:color/transparent"
        android:numColumns="3"
        android:stretchMode="columnWidth" >
    </GridView>
 </LinearLayout>
 //GridView的item的布局文件
 <?xml version="1.0" encoding="utf-8"?>
 <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:gravity="center"
    android:orientation="vertical" >
    <com.ryg.chapter_12.ui.SquareImageView
       android:id="@+id/image"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:scaleType="centerCrop"
        android:src="@drawable/image_default" />
 </LinearLayout>
```

也许读者已经注意到，GridView的item的布局文件中并没有采用ImageView，而是采用了一个叫SquareImageView的自定义控件。顾名思义，它的作用就是打造一个正方形的ImageView，这样整个照片墙看起来会比较整齐美观。要实现一个宽、高相等的ImageView是非常简单的一件事，只需要在它的onMeasure方法中稍微做一下处理，如下所示。

```
 public class SquareImageView extends ImageView {
    public SquareImageView(Context context) {
        super(context) ;
    }
    public SquareImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public SquareImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec) ;
    }
 }
```

可以看出，我们在SquareImageView的onMeasure方法中很巧妙地将heightMeasureSpec替换为widthMeasureSpec，这样什么都不用做就可以一个宽、高相等的ImageView了。关于View的测量等过程的介绍，请读者参看第4章的有关内容，这里不再赘述了。

接着需要实现一个BaseAdapter给GridView使用，下面的代码展示了ImageAdapter的实现细节，其中mUrList中存储的是图片的url：

```
private class ImageAdapter extends BaseAdapter {
     ...
     @Override
    public int getCount() {
         return mUrList.size();
     }
     @Override
     public String getItem(int position) {
         return mUrList.get(position);
     }
     @Override
     public long getItemId(int position) {
         return position;
     }
     @Override
     public View getView(int position, View convertView, ViewGroup parent) {
         ViewHolder holder = null;
         if(convertView == null) {
             convertView = mInflater.inflate(R.layout.image_list_item,
             parent, false);
             holder = new ViewHolder();
             holder.imageView = (ImageView) convertView.findViewById(R.
             id. image) ;
             convertView.setTag(holder) ;
         } else {
             holder = (ViewHolder) convertView.getTag();
         }
         ImageView imageView = holder.imageView;
         final String tag = (String)imageView.getTag();
         final String uri = getItem(position);
         if(!uri.equals(tag)) {
             imageView.setImageDrawable(mDefaultBitmapDrawable) ;
         }
         if(mIsGridViewIdle && mCanGetBitmapFromNetWork) {
             imageView.setTag(uri);
             mImageLoader.bindBitmap(uri, imageView, mImageWidth, mImagewidth) ;
         }
         return convertView;
     }
 }
```

从上述代码来看，ImageAdapter的实现过程非常简捷，这几乎是最简洁的BaseAdapter的实现了。但是简洁并不等于简单，getView方法中核心代码只有一句话，那就是：mImageLoader.bindBitmap(uri,imageView,mImageWidth,mImageWidth)。通过bindBitmap方法很轻松地将复杂的图片加载过程交给了ImageLoader，ImageLoader加载图片以后会把图片自动设置给imageView，而整个过程，包括内存缓存、磁盘缓存以及图片压缩等工作过程对ImageAdapter来说都是透明的。在这种设计思想下，ImageAdapter什么也不需要知道，因此这是一个极其轻量级的ImageAdapter。

接着将ImageAdapter设置给GridView，如下所示。到此为止一个绚丽的图片墙就大功告成了，是不是惊叹于如此简捷而又优美的实现过程呢？

```
mImageGridView = (GridView) findViewById(R.id.gridView1);
mImageAdapter = new ImageAdapter(this);
mImageGridView. setAdapter(mImageAdapter) ;
```

最后，看一下我们亲手打造的图片墙的效果图，如图12-1所示。是不是看起来很优美呢？

![图 12-1 采用 ImageLoader 实现的照片墙](images/figure-12-1.png)

图 12-1 采用 ImageLoader 实现的照片墙

另外，本节中的照片墙应用首次运行时会从网络中加载大量图片，这会消耗若干MB的流量，因此建议首次运行时选择WiFi环境，同时程序启动时也会有相应的提示，在非WiFi环境下，打开应用时会弹出如下提示，请读者运行时注意一下，避免消耗过多的流量。

```
if (!mIsWifi) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage("初次使用会从网络中下载大概 5MB 的图片，确认要下载吗？");
    builder.setTitle("注意");
    builder.setPositiveButton("是", new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mCanGetBitmapFromNetWork = true;
            mImageAdapter.notifyDataSetChanged();
        }
    });
    builder.setNegativeButton("否", null);
    builder.show();
}
```

### 12.3.2 优化列表的卡顿现象

这个问题困扰了很多开发者，其实答案很简单，不要在主线程中做太耗时的操作即可提高滑动的流畅度，可以从三个方面来说明这个问题。

首先，不要在getView中执行耗时操作。对于上面的例子来说，如果直接在getView方法中加载图片，肯定会导致卡顿，因为加载图片是一个耗时的操作，这种操作必须通过异步的方式来处理，就像ImageLoader实现的那样。

其次，控制异步任务的执行频率。这一点也很重要，对于列表来说，仅仅在getView中采用异步操作是不够的。考虑一种情况，以照片墙来说，在getView方法中会通过ImageLoader的bindBitmap方法来异步加载图片，但是如果用户刻意地频繁上下滑动，这就会在一瞬间产生上百个异步任务，这些异步任务会造成线程池的拥堵并随即带来大量的UI更新操作，这是没有意义的。由于一瞬间存在大量的UI更新操作，这些UI操作是运行在主线程的，这就会造成一定程度的卡顿。如何解决这个问题呢？可以考虑在列表滑动的时候停止加载图片，尽管这个过程是异步的，等列表停下来以后再加载图片仍然可以获得良好的用户体验。具体实现时，可以给ListView或者GridView设置setOnScrollListener，并在OnScrollListener的onScrollStateChanged方法中判断列表是否处于滑动状态，如果是的话就停止加载图片，如下所示。

```
public void onScrollStateChanged(AbsListView view, int scrollState) {
     if(scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
         mIsGridViewIdle = true;
         mImageAdapter.notifyDataSetChanged();
     } else {
         mIsGridViewIdle = false;
     }
 }
```

然后在getView方法中，仅当列表静止时才能加载图片，如下所示。

```
 if(mIsGridViewIdle && mCanGetBitmapFromNetWork) {
     imageView.setTag(uri) ;
    mImageLoader.bindBitmap(uri, imageView, mImageWidth, mImageWidth);
 }
```

一般来说，经过上面两个步骤，列表都不会有卡顿现象，但是在某些特殊情况下，列表还是会有偶尔的卡顿现象，这个时候还可以开启硬件加速。绝大多数情况下，硬件加速都可以解决莫名的卡顿问题，通过设置android:hardwareAccelerated="true"即可为Activity开启硬件加速。
