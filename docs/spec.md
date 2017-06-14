Key Monitor specifications
==========================


## Overview
Key Monitor [internal/temporary name] is a standalone third-party service that allows Signal users to opt in to being notified if their keys change on the Signal server.


## Rationale
End-to-end encryption allows two parties to communicate confidentially, provided they know each other's keys.

Sometimes, the keys associated with a particular user will change, for example if they get a new phone or reinstall the app. Their conversation partner can detect this change, but what should they do now?

If the app even bothers to inform the user that their partner's keys changed, they are faced with a choice that blocks all further actions: should the new keys be trusted?

The most secure response is to verify the other party's new key out-of-band, through a secure channel. In practice, few people do this, because they don't know how or why they should compare keys, they lack the means, or they simply don't believe it's necessary.

Another fundamental issue with this UX is that the person asked to make the decision lacks the necessary context. Did their partner get a new phone or reinstall the app? The person best positioned to answer those questions is the owner of the keys.

The purpose of Key Monitor is to inform the true account owner if their keys change, so that they can assess if the change is expected, verify the new keys, and inform their contacts if there are any problems.

## Goals

- A user should be able to opt in to monitoring by sending a message to the service's Signal number.
    - (MVP) The message should be the user's email address.
    - (Future work) KM should have (very basic) chatbot-style interactions, providing instructions if you don't know what you're doing
- KM will send an initial email to confirm the user's address and make sure they actually opted in to monitoring.
- At least once an hour, KM will query the Signal server for the user's key.
- If it detects that the key has changed, it will send an email to the user.
- (Future work) KM should be able to provide notifications through other channels (e.g., Twitter DMs)


## Threat model
Key Monitor is designed to address a specific threat: a third party takes control of your phone number (e.g., through social engineering or manipulating the network) and re-provisions Signal on their device, thus taking control of all further communications.

### Out of scope
#### Malicious Signal server
A server acting in an adversarial fashion can return different keys to different parties, thus tricking our service (or other clients) about which key is the true one. Without changes to the server's protocol (in particular, cryptographic guarantees about its results), fully addressing this threat may be difficult or impossible.
#### Malware on device
If malicious software takes control of your device, the attacker doesn't need to propagate new keys: they can just use your existing ones.
#### Malicious verification server (i.e., us going rogue)
The service will make no guarantees of accuracy (or availability). However, it will be open-source, so you can run your own!


## Non Goals

- KM is not designed to address the threats listed as out of scope above.
    - In particular, while there are some things we may do to make the Signal server "more honest," they will be insufficient against a determined adversary.
- KM will not provide mitigation if your phone number has been taken over. For example, it will not notify your contacts that you are not in control of your number.
    - (Future work) However, it may provide suggestions (e.g., for how to deal with your cell phone provider)
- KM will only monitor your own keys for changes, not those of others.
    - (Future work) We may want to relax this in the future, but there are privacy considerations to be dealt with.
