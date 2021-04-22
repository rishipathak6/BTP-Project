# BTP Project of Rishi Pathak

- This is the repository of the BTP of 170101054 Rishi Pathak.
- The topic of this project was Securing the video feed of IP surveillance cameras
- Like CCTV cameras, there are IP surveillance cameras that are connected to the internet and can be set up anywhere like your home or business, and they can transmit the video feed directly to your device.
- In recent years, there have been incidents where these systems have been hacked and used in botnets. Also, several reports show that the transmitted frames can be captured and have been used for privacy breaches.
- Therefore, in this project, I try to secure the video feed of these systems.
- For that, I make use of ChaCha20 stream cipher to selectively encrypt the frames and RSA cryptosystem to encrypt the key, nonce, and counter of ChaCha20.
- Read the attached report for more details.
