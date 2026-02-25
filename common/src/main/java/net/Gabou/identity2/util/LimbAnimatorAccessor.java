package net.Gabou.identity2.util;

public interface LimbAnimatorAccessor {
    void setPrevSpeed(float lastSpeed);

    void setSpeed(float speed);

    float getPrevSpeed();

    float getSpeed();

    void setPosition(float position);

    void setPositionScale(float positionScale);

    float getPosition();

    float getPositionScale();
}
