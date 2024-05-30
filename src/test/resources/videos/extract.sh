for f in *; do
  echo "File -> $f"
  dir=$(echo $f | awk -F _ '{ print $2 }')
  mkdir $dir
  docker run -v ${PWD}:/home/videos linuxserver/ffmpeg -i /home/videos/$f -q:v 5 -vf scale=h=256:force_original_aspect_ratio=decrease -vframes 400 /home/videos/$dir/frame%03d.jpg
done

