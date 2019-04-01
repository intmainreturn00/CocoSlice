/****************************************************************************
 Copyright (c) 2017-2018 Xiamen Yaji Software Co., Ltd.
 
 http://www.cocos2d-x.org
 
 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:
 
 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.
 
 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 ****************************************************************************/

#include "HelloWorldScene.h"
#include "SimpleAudioEngine.h"
#include <spine/spine-cocos2dx.h>
#include "spine/spine.h"
#include "Particle3D/CCParticleSystem3D.h"
#include "Particle3D/PU/CCPUParticleSystem3D.h"
#include "Particle3D/CCParticleSystem3D.h"

USING_NS_CC;
using namespace spine;

Scene *HelloWorld::createScene() {
    return HelloWorld::create();
}

bool HelloWorld::init() {
    if (!Scene::init()) {
        return false;
    }

    auto visibleSize = Director::getInstance()->getVisibleSize();
    Vec2 origin = Director::getInstance()->getVisibleOrigin();
    Vec2 center = Vec2(visibleSize.width / 2 + origin.x, visibleSize.height / 2 + origin.y);

    auto label = Label::createWithTTF("Hello World", "fonts/Marker Felt.ttf", 24);
    label->setPosition(Vec2(origin.x + visibleSize.width / 2,
                            origin.y + visibleSize.height - label->getContentSize().height));
    this->addChild(label);

    auto sprite = Sprite::create("HelloWorld.png");
    sprite->setPosition(center);

    this->addChild(sprite);

    auto skeletonNode = SkeletonAnimation::createWithJsonFile("spine/raptor-pro.json",
                                                              "spine/raptor.atlas", 0.5f);

    skeletonNode->setAnimation(0, "walk", true);

    skeletonNode->setAnchorPoint(Vec2(0, 0));
    skeletonNode->setPosition(Vec2(center.x / 2 + _contentSize.width / 2, 20));
    skeletonNode->setScale(0.2);
    addChild(skeletonNode);

    Size size = Director::getInstance()->getWinSize();
    auto _camera = Camera::createPerspective(30.0f, size.width / size.height, 1.0f, 1000.0f);
    _camera->setPosition3D(Vec3(0.0f, 0.0f, 100.0f));
    _camera->lookAt(Vec3(0.0f, 0.0f, 0.0f), Vec3(0.0f, 1.0f, 0.0f));
    _camera->setCameraFlag(CameraFlag::USER1);
    this->addChild(_camera);

    auto rootps = PUParticleSystem3D::create("lineStreak.pu", "pu_mediapack_01.material");
    rootps->setCameraMask((unsigned short) CameraFlag::USER1);
    rootps->setScale(4.0f);
    rootps->startParticleSystem();
    this->addChild(rootps, 0, 0x0001);

    scheduleUpdate();

    runAction(RepeatForever::create(Sequence::create(
            DelayTime::create(1),
            CallFunc::create([&]() { CCLOG("running"); }),
            nullptr
    )));

    return true;
}

void HelloWorld::update(float delta) {
    ParticleSystem3D *ps = static_cast<ParticleSystem3D *>(this->getChildByTag(0x0001));
    if (ps) {
        unsigned int count = 0;
        auto children = ps->getChildren();
        for (auto iter : children) {
            ParticleSystem3D *child = dynamic_cast<ParticleSystem3D *>(iter);
            if (child) {
                count += child->getAliveParticleCount();
            }
        }
    }
}

