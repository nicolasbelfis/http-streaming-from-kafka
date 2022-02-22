import 'bootstrap/dist/css/bootstrap.min.css';

export function TweetList(props) {
    let reversed = [...props.list].reverse()
    return (
        <div className="d-inline-block overflow-scroll me-2 bg-white ">
            <div className="small-height small">
                {reversed.map((value, index) => (index === 0) ?
                    <div className="h-25 fw-bold" key={parseInt(value.id)}>{value.text}</div> :
                    <div className="h-25" key={value.id}>{value.text}</div>)}
            </div>
        </div>
    );
}