import 'bootstrap/dist/css/bootstrap.min.css';

export function TweetList(props) {
    let fakeTweets = [
        {id: "1", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
        {id: "2", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
        {id: "3", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
        {id: "4", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
        {id: "5", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
        {id: "6", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
        {id: "7", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
        {id: "8", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
        {id: "9", text: "tweeet tweeettweeet tweeet tweeet tweeet tweeet tweeet..... tweeettweeet  tweeet tweeet"},
    ];

    return (
        <div className="d-inline-block overflow-scroll me-2 bg-white ">
            <div className="small-height">
                {props.list.map((value, index) => (index < props.list.length - 1) ? <p key={parseInt(value.id)}>{value.text}</p> :
                    <p className="fw-bold" key={value.id}>{value.text}</p>)}
            </div>
        </div>
    );
}