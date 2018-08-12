PROMOTED_JOB_NAME=$1
PROMOTED_NUMBER=$2

EXPOSED_PORT=8081
DOCKER_REGISTRY=localhost
DOCKER_REGISTRY_PORT=5000

IMAGE_NAME=$PROMOTED_JOB_NAME
IMAGE_VERSION=$PROMOTED_NUMBER
FULL_IMAGE_NAME=$DOCKER_REGISTRY:$DOCKER_REGISTRY_PORT/$IMAGE_NAME:$IMAGE_VERSION

docker pull $FULL_IMAGE_NAME

if docker service ls | grep $IMAGE_NAME;
        then docker service update --image $FULL_IMAGE_NAME $IMAGE_NAME
        else docker service create -d -p $EXPOSED_PORT:$EXPOSED_PORT --mode global --name $IMAGE_NAME $FULL_IMAGE_NAME
fi