gcloud run deploy myservice-v1 --image gcr.io/my-project/myimage:v1 --region us-central1
gcloud run deploy myservice-v2 --image gcr.io/my-project/myimage:v2 --region us-central1



# Create service negs

gcloud compute network-endpoint-groups create myservice-v1-neg \
    --region=us-central1 \
    --network-endpoint-type=serverless \
    --cloud-run-service=myservice-v1

gcloud compute network-endpoint-groups create myservice-v2-neg \
    --region=us-central1 \
    --network-endpoint-type=serverless \
    --cloud-run-service=myservice-v2


# Custom routing logic

gcloud compute url-maps add-path-matcher my-url-map \
  --default-service myservice-v1-backend \
  --path-matcher-name version-matcher \
  --new-hosts '*' \
  --route-rules='priority=0,matchRules=prefixMatch=/,headerMatch=X-Version:v2,service=myservice-v2-backend'
#

# Traffic splitting optional

gcloud run services update-traffic myservice \
    --to-revisions myservice-v1=80,myservice-v2=20



# Create backend services
gcloud compute backend-services create myservice-v1-backend \
    --protocol=HTTP \
    --port-name=http \
    --global

gcloud compute backend-services create myservice-v2-backend \
    --protocol=HTTP \
    --port-name=http \
    --global
# Attach NEG to backend services
gcloud compute backend-services add-backend myservice-v1-backend \
    --network-endpoint-group=myservice-v1-neg \
    --network-endpoint-group-region=us-central1 \
    --global
gcloud compute backend-services add-backend myservice-v2-backend \
    --network-endpoint-group=myservice-v2-neg \
    --network-endpoint-group-region=us-central1 \
    --global
# Create URL map
gcloud compute url-maps create my-url-map \
    --default-service myservice-v1-backend
# Add path matcher to URL map
gcloud compute url-maps add-path-matcher my-url-map \
    --default-service myservice-v1-backend \
    --path-matcher-name version-matcher \
    --new-hosts '*' \
    --route-rules='priority=0,matchRules=prefixMatch=/,headerMatch=X-Version:v2,service=myservice-v2-backend'
# Create target HTTP proxy
gcloud compute target-http-proxies create my-http-proxy \
    --url-map=my-url-map
# Create global forwarding rule
gcloud compute forwarding-rules create my-forwarding-rule \
    --global \
    --target-http-proxy=my-http-proxy \
    --ports=80 \
    --address=
    --description="Global forwarding rule for myservice"
# Create a global forwarding rule
gcloud compute forwarding-rules create my-forwarding-rule \
    --global \
    --target-http-proxy=my-http-proxy \
    --ports=80 \
    --address=


