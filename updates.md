# Spring Boot updates

https://phoenixnap.com/kb/letsencrypt-docker

```bash
docker-compose -f docker/docker-compose.yaml up
```
```bash
docker-compose run --rm certbot certonly --webroot --webroot-path /var/www/certbot/ --dry-run -d yolo.go.ro
```
```bash
 docker run -p 3000:8080 -p 11434:11434 -v ollama:/root/.ollama -v open-webui:/app/backend/data --name open-webui --restart always ghcr.io/open-webui/open-webui:ollama
```


```bash
docker tag docker.io/library/demo:0.0.1-SNAPSHOT mihaitatinta/http3-playground:0.0.1
```
```bash
 docker push mihaitatinta/http3-playground:0.0.1
 ```
```bash
ssh -p 443 -R0:localhost:443 -L4300:localhost:4300 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 PtOEDGWdSOA@a.pinggy.io 
```
```bash
ssh -p 443 -R0:localhost:8080 -L4300:localhost:4300 -o StrictHostKeyChecking=no -o ServerAliveInterval=30 PtOEDGWdSOA@a.pinggy.io 
```