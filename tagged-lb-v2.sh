#1 create cloud run serviuce with labels

# Deploy version A with labels
gcloud run deploy my-app-v1 \
  --image gcr.io/my-project/my-app:v1 \
  --platform managed \
  --region us-central1 \
  --update-labels version=v1,environment=production,team=backend

# Deploy version B with different labels
gcloud run deploy my-app-v2 \
  --image gcr.io/my-project/my-app:v2 \
  --platform managed \
  --region us-central1 \
  --update-labels version=v2,environment=staging,team=backend



# Create a load blancer with labels

# Create backend services for each version
gcloud compute backend-services create backend-v1 \
  --global \
  --load-balancing-scheme=EXTERNAL_MANAGED

gcloud compute backend-services create backend-v2 \
  --global \
  --load-balancing-scheme=EXTERNAL_MANAGED

# Add Cloud Run services as backends
gcloud compute backend-services add-backend backend-v1 \
  --global \
  --network-endpoint-group=my-app-v1-neg \
  --network-endpoint-group-region=us-central1

gcloud compute backend-services add-backend backend-v2 \
  --global \
  --network-endpoint-group=my-app-v2-neg \
  --network-endpoint-group-region=us-central1

# URL map with label based routing

# url-map.yaml
kind: compute#urlMap
name: label-based-lb
defaultService: projects/my-project/global/backendServices/backend-v1
hostRules:
- hosts:
  - my-app.example.com
  pathMatcher: main
pathMatchers:
- name: main
  defaultService: projects/my-project/global/backendServices/backend-v1
  routeRules:
  - priority: 1
    matchRules:
    - headerMatches:
      - headerName: X-Version
        exactMatch: v2
    service: projects/my-project/global/backendServices/backend-v2
  - priority: 2
    matchRules:
    - headerMatches:
      - headerName: X-Environment
        exactMatch: staging
    service: projects/my-project/global/backendServices/backend-v2


    #Cloud functions label based

    # Deploy function A with labels
gcloud functions deploy function-a \
  --runtime python39 \
  --trigger-http \
  --allow-unauthenticated \
  --region us-central1 \
  --update-labels version=stable,tier=premium,region=us

# Deploy function B with different labels  
gcloud functions deploy function-b \
  --runtime python39 \
  --trigger-http \
  --allow-unauthenticated \
  --region us-central1 \
  --update-labels version=beta,tier=standard,region=us

  # Cloud functions label based endpoint routing


  # openapi.yaml
swagger: '2.0'
info:
  title: Label-Based Function Router
  version: '1.0.0'
host: my-functions.endpoints.my-project.cloud.goog
schemes:
  - https
paths:
  /premium:
    get:
      operationId: premium-function
      x-google-backend:
        address: https://us-central1-my-project.cloudfunctions.net/function-a
        path_translation: APPEND_PATH_TO_ADDRESS
  /standard:
    get:
      operationId: standard-function  
      x-google-backend:
        address: https://us-central1-my-project.cloudfunctions.net/function-b
        path_translation: APPEND_PATH_TO_ADDRESS


# TRaffic based on labels

# Use Cloud Functions traffic allocation
gcloud functions deploy my-function \
  --runtime python39 \
  --trigger-http \
  --region us-central1 \
  --set-traffic-tags stable=80,beta=20


  #Intellegent routing logic

# Cloud Function router based on labels
import functions_framework
from google.cloud import functions_v1
import json

@functions_framework.http
def label_router(request):
    # Get request headers/parameters
    user_tier = request.headers.get('X-User-Tier', 'standard')
    region = request.headers.get('X-Region', 'us')
    
    # Query functions by labels
    client = functions_v1.CloudFunctionsServiceClient()
    parent = f"projects/{PROJECT_ID}/locations/{LOCATION}"
    
    functions = client.list_functions(parent=parent)
    
    # Route based on labels
    target_function = None
    for func in functions:
        labels = func.labels or {}
        if (labels.get('tier') == user_tier and 
            labels.get('region') == region):
            target_function = func.name
            break
    
    if target_function:
        # Forward request to selected function
        return forward_request(target_function, request)
    else:
        return {'error': 'No matching function found'}, 404


