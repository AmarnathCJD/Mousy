package main

import (
	"fmt"
	"log"
	"math"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/go-vgo/robotgo"
	"github.com/gorilla/websocket"
)

type Mouse struct {
	prevAX float64
	prevAY float64
	prevAZ float64

	x int
	y int

	delay  int
	ticker *time.Ticker
}

var m = Mouse{delay: 20, ticker: time.NewTicker(2)}

func similar(x, y float64) bool {
	return math.Abs(x-y) < 0.5
}

func (m *Mouse) controlMouse(xForce, yForce, zForce float64) {
	if (xForce == m.prevAX) && (yForce == m.prevAY) || (similar(xForce, m.prevAX) && similar(yForce, m.prevAY)) {
		return
	}
	// robotgo.Move(500, 500)
	// return
	// Get current mouse position
	if (m.x == 0) && (m.y == 0) {
		cx, cy := robotgo.Location()
		m.x = cx
		m.y = cy
	}

	cx := m.x
	cy := m.y

	// mouse.move(-X*6,Y*6)
	robotgo.Move(cx+int(xForce*10), cy-int(yForce*10))
	m.prevAX = xForce
	m.prevAY = yForce
	m.x = cx + int(xForce*8)
	m.y = cy - int(yForce*8)
	return
	fmt.Println("Current X: ", cx)
	fmt.Println("Current Y: ", cy)
	// return

	// // If this is the first time, set the previous acceleration values
	if (m.prevAX == 0) && (m.prevAY == 0) {
		m.prevAX = xForce
		m.prevAY = yForce
		return
	}

	// Get screen resolution
	//resx, resy := robotgo.GetScreenSize()

	// // set to center of screen
	// resx = resx / 2
	// resy = resy / 2

	// robotgo.MoveMouse(resx, resy)
	// return

	scaleX := calcScale(int(xForce), m.prevAX)
	scaleY := calcScale(int(yForce), m.prevAY)

	// if x +ve then make scaleX -ve
	if xForce > 0 {
		scaleX = -scaleX
	}
	// if y +ve then make scaleY -ve
	if yForce > 0 {
		scaleY = -scaleY
	}

	fmt.Println("ScaleX: ", scaleX)
	fmt.Println("ScaleY: ", scaleY)

	robotgo.SetDelay(1)

	if (scaleX == 0) && (scaleY == 0) {
		return
	}
	fmt.Println("Moving mouse by: ", scaleX, scaleY, "ie. ", cx+scaleX, cy+scaleY, "cx: ", cx, "cy: ", cy)
	if (cx+scaleX) < 0 || (cy+scaleY) < 0 {
		return
	}

	if (cx+scaleX) > 1920 || (cy+scaleY) > 1080 {
		return
	}

	if (scaleX != 0) && (scaleY == 0) {
		robotgo.Move(cx+scaleX, cy)
	} else if (scaleX == 0) && (scaleY != 0) {
		robotgo.Move(cx, cy+scaleY)
	} else {
		robotgo.Move(cx+scaleX, cy+scaleY)
	}
}

func calcScale(x int, prev float64) int {
	// closer to 0, the slower the mouse moves
	// closer to 5, the faster the mouse moves
	// if abs of prev-x is less than 0.5, then return 0
	if math.Abs(float64(int(prev)-x)) < 0.5 {
		return 0
	}

	scaleMap := map[int]int{
		-5: 40,
		-4: 30,
		-3: 20,
		-2: 10,
		-1: 5,
		0:  0,
		1:  5,
		2:  10,
		3:  20,
		4:  30,
		5:  40,
	}

	closest := 0
	closestDiff := 1000

	for k, _ := range scaleMap {
		diff := x - k
		if diff < 0 {
			diff = -diff
		}

		if diff < closestDiff {
			closestDiff = diff
			closest = k
		}
	}

	return scaleMap[closest]
}

func scale() int {
	//x, y := robotgo.GetScreenSize()

	return 2
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

func handleConnections(w http.ResponseWriter, r *http.Request) {
	ws, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Fatal(err)
	}
	defer ws.Close()

	ws.WriteMessage(websocket.TextMessage, []byte("Connected to server"))

	for {
		_, msg, err := ws.ReadMessage()
		if err != nil {
			log.Println(err)
			return
		}

		values := strings.Split(string(msg), ",")
		x, _ := strconv.ParseFloat(values[0], 64)
		y, _ := strconv.ParseFloat(values[1], 64)
		z, _ := strconv.ParseFloat(values[2], 64)

		// curr_x, curr_y := robotgo.Location()
		// mouse := Mouse{x: curr_x, y: curr_y}

		m.controlMouse(x, y, z)
	}
}

func main() {

	robotgo.MouseSleep = 0
	robotgo.SetDelay(1 / 2)

	http.HandleFunc("/acclerate", func(w http.ResponseWriter, r *http.Request) {
		if r.Method == "POST" {
			// x := r.FormValue("x")
			// y := r.FormValue("y")
			// acceleration := r.FormValue("acceleration")

			// x_axis, _ := strconv.ParseFloat(x, 64)
			// y_axis, _ := strconv.ParseFloat(y, 64)
			// acceleration_value, _ := strconv.ParseFloat(acceleration, 64)

			// calc(x_axis, y_axis, acceleration_value)
		} else {
			// x := r.URL.Query().Get("x")
			// y := r.URL.Query().Get("y")
			// acceleration := r.URL.Query().Get("z")

			// x_axis, _ := strconv.ParseFloat(x, 64)
			// y_axis, _ := strconv.ParseFloat(y, 64)
			// acceleration_value, _ := strconv.ParseFloat(acceleration, 64)

			// calc(x_axis, y_axis, acceleration_value)
		}
	})

	http.HandleFunc("/ws", handleConnections)
	log.Println("Mouse controller is running on port 8080")
	http.ListenAndServe("0.0.0.0:8080", nil)
}
